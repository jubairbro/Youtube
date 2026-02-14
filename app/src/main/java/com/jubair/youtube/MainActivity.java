package com.jubair.youtube;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.jubair.youtube.managers.AdBlocker;
import com.jubair.youtube.services.BackgroundAudioService;
import com.jubair.youtube.utils.GoodTubeScript;
import com.jubair.youtube.ui.DialogManager;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;
    private FrameLayout mFullscreenContainer;
    private RelativeLayout mOfflineLayout;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private AudioManager audioManager;

    // JavaScript Bridge
    public class WebAppInterface {
        @JavascriptInterface
        public void onVideoPlay() {
            updateServiceState(true);
            requestAudioFocus();
        }

        @JavascriptInterface
        public void onVideoPause() {
            updateServiceState(false);
        }
    }

    private void updateServiceState(boolean isPlaying) {
        Intent serviceIntent = new Intent(this, BackgroundAudioService.class);
        serviceIntent.putExtra("IS_PLAYING", isPlaying);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    // মিডিয়া কন্ট্রোল রিসিভার
    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String command = intent.getStringExtra("COMMAND");
            if (command != null) {
                switch (command) {
                    case "PLAY":
                        myWebView.evaluateJavascript("document.querySelector('video').play();", null);
                        break;
                    case "PAUSE":
                        myWebView.evaluateJavascript("document.querySelector('video').pause();", null);
                        break;
                    case "NEXT":
                        myWebView.evaluateJavascript("document.querySelector('.ytp-next-button').click();", null);
                        break;
                    case "PREV":
                        myWebView.evaluateJavascript("document.querySelector('.ytp-prev-button').click();", null);
                        break;
                }
            }
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // পারমিশন রিকোয়েস্ট
        checkPermissions();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
        IntentFilter filter = new IntentFilter("MEDIA_CONTROL");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(serviceReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(serviceReceiver, filter);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        myWebView = findViewById(R.id.main_webview);
        mFullscreenContainer = findViewById(R.id.fullscreen_container);
        mOfflineLayout = findViewById(R.id.offline_layout);
        Button btnRetry = findViewById(R.id.btn_retry);

        initWebView();
        DialogManager.showWelcomeDialog(this);

        btnRetry.setOnClickListener(v -> myWebView.reload());
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.RECORD_AUDIO}, 101);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 101);
        }
    }

    private void requestAudioFocus() {
        if (audioManager != null) {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void initWebView() {
        WebSettings s = myWebView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36");

        myWebView.addJavascriptInterface(new WebAppInterface(), "Android");

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (AdBlocker.isAd(request.getUrl().toString())) return AdBlocker.createEmptyResponse();
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl(GoodTubeScript.getInjectScript());
                // Unmute & Visibility Hack
                view.loadUrl("javascript:(function(){ " +
                    "Object.defineProperty(document, 'hidden', { value: false, writable: false }); " +
                    "Object.defineProperty(document, 'visibilityState', { value: 'visible', writable: false }); " +
                    "setInterval(function(){ " +
                    "  var v = document.querySelector('video'); if(v && v.muted) v.muted = false; " +
                    "  var b = document.querySelector('.ytp-unmute'); if(b) b.click(); " +
                    "}, 1000); " +
                    "})()");
            }
        });

        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) { request.grant(request.getResources()); }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                mCustomView = view;
                myWebView.setVisibility(View.GONE);
                mFullscreenContainer.setVisibility(View.VISIBLE);
                mFullscreenContainer.addView(view);
                mCustomViewCallback = callback;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }

            @Override
            public void onHideCustomView() {
                myWebView.setVisibility(View.VISIBLE);
                mFullscreenContainer.setVisibility(View.GONE);
                mFullscreenContainer.removeView(mCustomView);
                mCustomView = null;
                mCustomViewCallback.onCustomViewHidden();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        });

        myWebView.loadUrl("https://m.youtube.com");
    }

    @Override
    protected void onPause() { super.onPause(); }

    @Override
    protected void onDestroy() {
        try { unregisterReceiver(serviceReceiver); } catch (Exception e) {}
        stopService(new Intent(this, BackgroundAudioService.class));
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mCustomView != null) {
            myWebView.getWebChromeClient().onHideCustomView();
        } else if (myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            moveTaskToBack(true);
        }
    }
}

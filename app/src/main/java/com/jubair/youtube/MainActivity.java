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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.jubair.youtube.managers.AdBlocker;
import com.jubair.youtube.services.BackgroundAudioService;
import com.jubair.youtube.utils.GoodTubeScript;
import com.jubair.youtube.ui.DialogManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;
    private FrameLayout mFullscreenContainer;
    private RelativeLayout mOfflineLayout;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private AudioManager audioManager;

    // JavaScript Interface
    public class WebAppInterface {
        @JavascriptInterface
        public void onVideoPlay() {
            // সার্ভিস স্টার্ট এবং অডিও ফোকাস
            if (!BackgroundAudioService.isServiceRunning) {
                startAudioService(true);
            } else {
                updateServiceState(true);
            }
            requestAudioFocus();
        }

        @JavascriptInterface
        public void onVideoPause() {
            updateServiceState(false);
        }
    }

    private void startAudioService(boolean isPlaying) {
        Intent serviceIntent = new Intent(this, BackgroundAudioService.class);
        serviceIntent.putExtra("IS_PLAYING", isPlaying);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void updateServiceState(boolean isPlaying) {
        Intent serviceIntent = new Intent(this, BackgroundAudioService.class);
        serviceIntent.putExtra("IS_PLAYING", isPlaying);
        startService(serviceIntent);
    }

    // Media Control Receiver (বাটন ক্লিকের জন্য)
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
                        myWebView.evaluateJavascript("if(document.querySelector('.ytp-next-button')) document.querySelector('.ytp-next-button').click();", null);
                        break;
                    case "PREV":
                        myWebView.evaluateJavascript("if(document.querySelector('.ytp-prev-button')) document.querySelector('.ytp-prev-button').click();", null);
                        break;
                }
            }
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ১. পারমিশন চেক এবং রিকোয়েস্ট (Mic & Notification)
        checkAndRequestPermissions();

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

    private void checkAndRequestPermissions() {
        List<String> permissions = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 101);
        }
    }

    private void requestAudioFocus() {
        if (audioManager != null) {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void initWebView() {
        WebSettings settings = myWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        myWebView.addJavascriptInterface(new WebAppInterface(), "Android");
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (AdBlocker.isAd(request.getUrl().toString())) return AdBlocker.createEmptyResponse();
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl(GoodTubeScript.getInjectScript());
                super.onPageFinished(view, url);
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (errorCode == -2) {
                    myWebView.setVisibility(View.GONE);
                    mOfflineLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        myWebView.setWebChromeClient(new WebChromeClient() {
            // ২. ভয়েস সার্চ পারমিশন হ্যান্ডলিং
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    request.grant(request.getResources());
                } else {
                    // অ্যাপে পারমিশন না থাকলে আবার চাইবে
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 102);
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (mCustomView != null) { callback.onCustomViewHidden(); return; }
                mCustomView = view;
                myWebView.setVisibility(View.GONE);
                mFullscreenContainer.setVisibility(View.VISIBLE);
                mFullscreenContainer.addView(view);
                mCustomViewCallback = callback;
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }

            @Override
            public void onHideCustomView() {
                if (mCustomView == null) return;
                myWebView.setVisibility(View.VISIBLE);
                mFullscreenContainer.setVisibility(View.GONE);
                mFullscreenContainer.removeView(mCustomView);
                mCustomView = null;
                mCustomViewCallback.onCustomViewHidden();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
            WebChromeClient wcc = myWebView.getWebChromeClient();
            if (wcc != null) wcc.onHideCustomView();
            return;
        }
        if (myWebView.canGoBack()) myWebView.goBack();
        else moveTaskToBack(true);
    }
}

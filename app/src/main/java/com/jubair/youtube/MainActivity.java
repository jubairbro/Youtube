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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

    // JavaScript Bridge (Video State Detector)
    public class WebAppInterface {
        @JavascriptInterface
        public void onVideoPlay() {
            // ভিডিও চললে সার্ভিস স্টার্ট হবে এবং অডিও ফোকাস নিবে
            Intent serviceIntent = new Intent(MainActivity.this, BackgroundAudioService.class);
            serviceIntent.putExtra("IS_PLAYING", true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            requestAudioFocus();
        }

        @JavascriptInterface
        public void onVideoPause() {
            Intent serviceIntent = new Intent(MainActivity.this, BackgroundAudioService.class);
            serviceIntent.putExtra("IS_PLAYING", false);
            startService(serviceIntent);
        }
    }

    // Media Controls Receiver
    private final BroadcastReceiver mediaReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("ACTION_PLAY".equals(action)) myWebView.evaluateJavascript("window.handleMediaKey('play')", null);
            else if ("ACTION_PAUSE".equals(action)) myWebView.evaluateJavascript("window.handleMediaKey('pause')", null);
            else if ("ACTION_NEXT".equals(action)) myWebView.evaluateJavascript("window.handleMediaKey('next')", null);
            else if ("ACTION_PREV".equals(action)) myWebView.evaluateJavascript("window.handleMediaKey('prev')", null);
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ১. নোটিফিকেশন পারমিশন চাওয়া (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // ২. অডিও ফোকাস সেটআপ
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        requestAudioFocus();

        // ৩. রিসিভার রেজিস্টার
        IntentFilter filter = new IntentFilter();
        filter.addAction("ACTION_PLAY");
        filter.addAction("ACTION_PAUSE");
        filter.addAction("ACTION_NEXT");
        filter.addAction("ACTION_PREV");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(mediaReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mediaReceiver, filter);
        }

        // সার্ভিস স্টার্ট (ডিফল্ট)
        startService(new Intent(this, BackgroundAudioService.class));
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

    private void requestAudioFocus() {
        if (audioManager != null) {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void initWebView() {
        WebSettings settings = myWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false); // অটোপ্লে এবং আনমিউট ফিক্স
        
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
                // এক্সট্রা আনমিউট স্ক্রিপ্ট
                view.loadUrl("javascript:(function(){ var unmuteBtn = document.querySelector('.ytp-unmute'); if(unmuteBtn) unmuteBtn.click(); })();");
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
            @Override
            public void onPermissionRequest(PermissionRequest request) { request.grant(request.getResources()); }

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
    protected void onPause() { 
        super.onPause(); 
        // WebView Pause হতে দিচ্ছি না
    }

    @Override
    protected void onDestroy() {
        try { unregisterReceiver(mediaReceiver); } catch (Exception e) {}
        stopService(new Intent(this, BackgroundAudioService.class));
        super.onDestroy();
    }

    // ৪. PiP রিমুভ করা হয়েছে (onUserLeaveHint ডিলিট করে দিয়েছি)

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

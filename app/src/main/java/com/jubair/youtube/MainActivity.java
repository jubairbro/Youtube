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

    // JavaScript Bridge (ভিডিওর আসল অবস্থা ডিটেক্ট করবে)
    public class WebAppInterface {
        @JavascriptInterface
        public void onVideoPlay() {
            // ভিডিও চললে সার্ভিস স্টার্ট হবে
            if (!isServiceRunning) {
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

    private boolean isServiceRunning = false;

    private void startAudioService(boolean isPlaying) {
        Intent serviceIntent = new Intent(this, BackgroundAudioService.class);
        serviceIntent.putExtra("IS_PLAYING", isPlaying);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        isServiceRunning = true;
    }

    private void updateServiceState(boolean isPlaying) {
        Intent serviceIntent = new Intent(this, BackgroundAudioService.class);
        serviceIntent.putExtra("IS_PLAYING", isPlaying);
        startService(serviceIntent);
    }

    // সার্ভিস থেকে বাটন ক্লিকের সিগনাল রিসিভ করা
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
        
        // নোটিফিকেশন পারমিশন
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // রিসিভার রেজিস্টার
        IntentFilter filter = new IntentFilter("MEDIA_CONTROL");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(serviceReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(serviceReceiver, filter);
        }

        // দ্রষ্টব্য: onCreate এ সার্ভিস স্টার্ট করা বাদ দিয়েছি

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
                // এক্সট্রা আনমিউট স্ক্রিপ্ট
                view.loadUrl("javascript:(function(){ " +
                        " var unmuteBtn = document.querySelector('.ytp-unmute');" +
                        " if(unmuteBtn) unmuteBtn.click();" +
                        " })();");
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
        try { unregisterReceiver(serviceReceiver); } catch (Exception e) {}
        // অ্যাপ পুরোপুরি কিল করলে সার্ভিস বন্ধ হবে (অপশনাল)
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

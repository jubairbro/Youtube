package com.jubair.youtube;

import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
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

import com.jubair.youtube.managers.AdBlocker;
import com.jubair.youtube.services.BackgroundAudioService;
import com.jubair.youtube.utils.ScriptLoader; // নতুন লোডার
import com.jubair.youtube.ui.DialogManager;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;
    private FrameLayout mFullscreenContainer;
    private RelativeLayout mOfflineLayout;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;

    // JavaScript Bridge
    public class WebAppInterface {
        @JavascriptInterface
        public void onVideoPlay() {
            Intent serviceIntent = new Intent(MainActivity.this, BackgroundAudioService.class);
            serviceIntent.putExtra("IS_PLAYING", true);
            startService(serviceIntent);
        }

        @JavascriptInterface
        public void onVideoPause() {
            Intent serviceIntent = new Intent(MainActivity.this, BackgroundAudioService.class);
            serviceIntent.putExtra("IS_PLAYING", false);
            startService(serviceIntent);
        }
    }

    // Media Receiver
    private final BroadcastReceiver mediaReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // GoodTube Player কন্ট্রোল করা
            if ("ACTION_PLAY".equals(action)) myWebView.evaluateJavascript("document.querySelector('video').play();", null);
            else if ("ACTION_PAUSE".equals(action)) myWebView.evaluateJavascript("document.querySelector('video').pause();", null);
            else if ("ACTION_NEXT".equals(action)) myWebView.evaluateJavascript("document.querySelector('.ytp-next-button').click();", null);
            else if ("ACTION_PREV".equals(action)) myWebView.evaluateJavascript("document.querySelector('.ytp-prev-button').click();", null);
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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

    private void initWebView() {
        WebSettings settings = myWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true); // Assets রিড করার জন্য
        
        myWebView.addJavascriptInterface(new WebAppInterface(), "Android");
        
        // Desktop User Agent (GoodTube ডেস্কটপ মোডে সবচেয়ে ভালো কাজ করে)
        // তবে আমরা মোবাইল সাইজে ফিট করার জন্য CSS দিয়ে সাইজ ঠিক করব
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // গুগলের ভিডিও সার্ভার এবং এড সার্ভার চেক করা
                if (AdBlocker.isAd(request.getUrl().toString())) {
                    return AdBlocker.createEmptyResponse();
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // ১. GoodTube Engine ইনজেক্ট করা (Assets থেকে)
                String goodTubeScript = ScriptLoader.loadGoodTube(MainActivity.this);
                if (!goodTubeScript.isEmpty()) {
                    view.loadUrl(goodTubeScript);
                }

                // ২. এক্সট্রা হ্যাক (UI ক্লিনআপ এবং ব্যাকগ্রাউন্ড প্লে)
                view.loadUrl("javascript:(function() {" +
                        // ভিডিও প্লে হলে জাভা অ্যাপকে জানানো
                        "var video = document.querySelector('video');" +
                        "if(video) {" +
                        "   video.addEventListener('play', function() { Android.onVideoPlay(); });" +
                        "   video.addEventListener('pause', function() { Android.onVideoPause(); });" +
                        "}" +
                        // হিউম্যান ভেরিফিকেশন পপআপ রিমুভ
                        "var popup = document.querySelector('.yt-consent-banner');" +
                        "if(popup) popup.remove();" +
                        "})()");
                
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
    protected void onPause() { super.onPause(); } 

    @Override
    protected void onDestroy() {
        try { unregisterReceiver(mediaReceiver); } catch (Exception e) {}
        stopService(new Intent(this, BackgroundAudioService.class));
        super.onDestroy();
    }

    @Override
    protected void onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Rational aspectRatio = new Rational(16, 9);
                PictureInPictureParams.Builder params = new PictureInPictureParams.Builder();
                params.setAspectRatio(aspectRatio);
                enterPictureInPictureMode(params.build());
            } catch (Exception e) {}
        }
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

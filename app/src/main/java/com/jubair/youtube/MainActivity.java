package com.jubair.youtube;

import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.View;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.jubair.youtube.managers.AdBlocker;
import com.jubair.youtube.services.BackgroundAudioService;
import com.jubair.youtube.ui.DialogManager;
import com.jubair.youtube.utils.ScriptInjector;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;
    private FrameLayout mFullscreenContainer;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ১. ব্যাকগ্রাউন্ড সার্ভিস চালু করা (গুরুত্বপূর্ণ)
        startService(new Intent(this, BackgroundAudioService.class));
        
        // ২. স্ক্রিন অন রাখা (ভিডিও দেখার সময়)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        myWebView = findViewById(R.id.main_webview);
        mFullscreenContainer = findViewById(R.id.fullscreen_container);

        initWebView();
        
        // ৩. ডায়লগ শো করা (যদি ইউজার অফ না করে থাকে)
        DialogManager.showCyberpunkDialog(this);
    }

    private void initWebView() {
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false); // অটো প্লে এবং ব্যাকগ্রাউন্ড অডিওর জন্য
        
        // Desktop/Premium User Agent
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36");

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // ৪. অ্যাড ব্লকার ক্লাস কল করা
                if (AdBlocker.isAd(request.getUrl().toString())) {
                    return AdBlocker.createEmptyResponse();
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // ৫. স্ক্রিপ্ট ইনজেক্ট করা (UI ক্লিনআপ)
                view.loadUrl(ScriptInjector.getInjectScript());
                super.onPageFinished(view, url);
            }
        });

        myWebView.setWebChromeClient(new WebChromeClient() {
            // ৬. পারমিশন (মাইক্রোফোন ইত্যাদি) অটো অ্যালাউ
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }

            // ৭. ফুলস্ক্রিন এবং রোটেশন লজিক
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (mCustomView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                mCustomView = view;
                myWebView.setVisibility(View.GONE);
                mFullscreenContainer.setVisibility(View.VISIBLE);
                mFullscreenContainer.addView(view);
                mCustomViewCallback = callback;
                
                // ল্যান্ডস্কেপ মোড ফোর্স করা
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
                
                // পোর্ট্রেট মোড ফেরত আনা
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        });

        if (myWebView.getUrl() == null) {
            myWebView.loadUrl("https://m.youtube.com");
        }
    }

    // ৮. ব্যাকগ্রাউন্ড প্লে লজিক (সবচেয়ে ট্রিকি পার্ট)
    @Override
    protected void onPause() {
        // সুপার কল করলেও আমরা ওয়েবভিউ পজ করব না
        super.onPause();
        // myWebView.onPause(); // এই লাইনটা ইচ্ছা করে বাদ দেওয়া হয়েছে
        
        // PiP মোডে না থাকলে ব্যাকগ্রাউন্ড সার্ভিস চালু থাকবে
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isInPictureInPictureMode()) {
            // অ্যাপ মিনিমাইজ হলেও গান চলবে
        }
    }

    @Override
    protected void onDestroy() {
        // অ্যাপ পুরোপুরি বন্ধ করলে সার্ভিস থামবে
        stopService(new Intent(this, BackgroundAudioService.class));
        super.onDestroy();
    }

    // ৯. অটো PiP (হোম বাটন চাপলে)
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Rational aspectRatio = new Rational(16, 9);
                PictureInPictureParams.Builder params = new PictureInPictureParams.Builder();
                params.setAspectRatio(aspectRatio);
                enterPictureInPictureMode(params.build());
            } catch (Exception e) {
                // PiP সাপোর্ট না করলে কিছু হবে না, ব্যাকগ্রাউন্ড অডিও চলবে
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mCustomView != null) {
            WebChromeClient wcc = myWebView.getWebChromeClient();
            if (wcc != null) wcc.onHideCustomView();
            return;
        }
        if (myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            // ব্যাকগ্রাউন্ডে পাঠিয়ে দাও, বন্ধ করো না
            moveTaskToBack(true);
        }
    }
}

package com.jubair.youtube;

import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.jubair.youtube.utils.ScriptInjector;
import com.jubair.youtube.ui.DialogManager;
import com.jubair.youtube.utils.CrashHandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;
    private FrameLayout mFullscreenContainer;
    private RelativeLayout mOfflineLayout;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    // বাটন ভেরিয়েবল রিমুভ করা হয়েছে

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
        
        // স্ক্রিন অন রাখা
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        myWebView = findViewById(R.id.main_webview);
        mFullscreenContainer = findViewById(R.id.fullscreen_container);
        mOfflineLayout = findViewById(R.id.offline_layout);
        Button btnRetry = findViewById(R.id.btn_retry);

        initWebView();
        
        // অ্যাপ ওপেন হলে ডায়লগ দেখাবে
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isInPictureInPictureMode()) {
            DialogManager.showCyberpunkDialog(this);
            showSenseiToast();
        }

        btnRetry.setOnClickListener(v -> checkNetworkAndLoad());
        checkNetworkAndLoad();
    }

    // অ্যাপ আইকনে ক্লিক করলে যাতে রিস্টার্ট না হয়
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    // --- AUTO PiP & BACKGROUND PLAY ---
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
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
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            mOfflineLayout.setVisibility(View.GONE);
        } else {
            if (myWebView.getUrl() == null) checkNetworkAndLoad();
        }
    }

    // --- BACKGROUND PLAY FIX (CRITICAL) ---
    @Override
    protected void onPause() {
        super.onPause();
        // myWebView.onPause(); // এটা অফ রাখলে ব্যাকগ্রাউন্ডে অডিও চলবে
    }

    @Override
    protected void onResume() {
        myWebView.onResume();
        super.onResume();
    }

    private void checkNetworkAndLoad() {
        if (isNetworkAvailable()) {
            mOfflineLayout.setVisibility(View.GONE);
            myWebView.setVisibility(View.VISIBLE);
            if (myWebView.getUrl() == null) myWebView.loadUrl("https://m.youtube.com");
        } else {
            myWebView.setVisibility(View.GONE);
            mOfflineLayout.setVisibility(View.VISIBLE);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    private void initWebView() {
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString().toLowerCase();
                
                // নেটওয়ার্ক লেভেল এড ব্লকিং
                if (url.contains("googleads") || 
                    url.contains("doubleclick") || 
                    url.contains("adservice") || 
                    url.contains("googlesyndication") ||
                    url.contains("youtube.com/api/stats/ads")) {
                    return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl(ScriptInjector.getRemoveAdsScript());
                super.onPageFinished(view, url);
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
            }
        });
    }

    private void showSenseiToast() {
        try {
            TextView text = new TextView(this);
            text.setText("System: Jubair Sensei");
            text.setTextColor(Color.parseColor("#00FF00"));
            text.setTypeface(null, android.graphics.Typeface.BOLD);
            text.setBackgroundResource(R.drawable.neon_bg);
            text.setPadding(30, 15, 30, 15);
            Toast toast = new Toast(getApplicationContext());
            toast.setView(text);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.show();
        } catch (Exception e) {}
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
            moveTaskToBack(true);
        }
    }
}

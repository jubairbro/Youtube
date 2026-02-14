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

import com.jubair.youtube.managers.AdBlocker;
import com.jubair.youtube.services.BackgroundAudioService;
import com.jubair.youtube.ui.DialogManager;
import com.jubair.youtube.utils.ScriptInjector;
import com.jubair.youtube.utils.CrashHandler;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;
    private FrameLayout mFullscreenContainer;
    private RelativeLayout mOfflineLayout;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;

    // মিডিয়া কন্ট্রোল রিসিভার
    private final BroadcastReceiver mediaReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ACTION_PLAY".equals(intent.getAction())) {
                myWebView.evaluateJavascript("document.querySelector('video').play();", null);
            } else if ("ACTION_PAUSE".equals(intent.getAction())) {
                myWebView.evaluateJavascript("document.querySelector('video').pause();", null);
            }
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
        
        // মিডিয়া রিসিভার রেজিস্টার
        IntentFilter filter = new IntentFilter();
        filter.addAction("ACTION_PLAY");
        filter.addAction("ACTION_PAUSE");
        registerReceiver(mediaReceiver, filter);
        
        startService(new Intent(this, BackgroundAudioService.class));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        myWebView = findViewById(R.id.main_webview);
        mFullscreenContainer = findViewById(R.id.fullscreen_container);
        mOfflineLayout = findViewById(R.id.offline_layout);
        Button btnRetry = findViewById(R.id.btn_retry);

        initWebView();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isInPictureInPictureMode()) {
            Toast.makeText(this, "Jubair Sensei", Toast.LENGTH_SHORT).show();
            DialogManager.showWelcomeDialog(this);
        }

        btnRetry.setOnClickListener(v -> checkNetworkAndLoad());
        checkNetworkAndLoad();
    }

    private void initWebView() {
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false); 
        
        // Vanced এর মতো ফিল পেতে ডেস্কটপ মোড ইউজ করা যাবে না, মোবাইল মোডই বেটার তবে ইউজার এজেন্ট ফিক্সড
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36");

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (AdBlocker.isAd(request.getUrl().toString())) {
                    return AdBlocker.createEmptyResponse();
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl(ScriptInjector.getInjectScript());
                super.onPageFinished(view, url);
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                myWebView.setVisibility(View.GONE);
                mOfflineLayout.setVisibility(View.VISIBLE);
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

        if (myWebView.getUrl() == null) {
            myWebView.loadUrl("https://m.youtube.com");
        }
    }

    private void checkNetworkAndLoad() {
        if (isNetworkAvailable()) {
            mOfflineLayout.setVisibility(View.GONE);
            myWebView.setVisibility(View.VISIBLE);
            if (myWebView.getUrl() == null) myWebView.loadUrl("https://m.youtube.com");
            else myWebView.reload();
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

    @Override
    protected void onPause() {
        super.onPause();
        // WebView Pause হতে দিচ্ছি না
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mediaReceiver);
        stopService(new Intent(this, BackgroundAudioService.class));
        super.onDestroy();
    }

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
            // PiP মোডে এক্সট্রা সব এলিমেন্ট হাইড হয়ে যাবে (CSS এর কারণে)
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
            moveTaskToBack(true);
        }
    }
}

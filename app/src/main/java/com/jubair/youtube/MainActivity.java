package com.jubair.youtube;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.jubair.youtube.utils.ScriptInjector;
import com.jubair.youtube.ui.DialogManager;
import com.jubair.youtube.utils.CrashHandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;
    private FrameLayout mFullscreenContainer;
    private RelativeLayout mOfflineLayout;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private Button btnMenuTrigger; 
    private boolean isAudioMode = true;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
        
        // স্ক্রিন যাতে হুট করে বন্ধ না হয়
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        myWebView = findViewById(R.id.main_webview);
        mFullscreenContainer = findViewById(R.id.fullscreen_container);
        mOfflineLayout = findViewById(R.id.offline_layout);
        btnMenuTrigger = findViewById(R.id.btn_menu_trigger);
        Button btnRetry = findViewById(R.id.btn_retry);

        initWebView();
        
        // অ্যাপ চালু হলে ডায়লগ দেখাবে (যদি PiP না থাকে)
        if (!isInPictureInPictureMode()) {
            DialogManager.showCyberpunkDialog(this);
            showSenseiToast();
        }

        btnMenuTrigger.setOnClickListener(v -> showControlCenter());
        btnRetry.setOnClickListener(v -> checkNetworkAndLoad());
        checkNetworkAndLoad();
    }

    // অ্যাপ আইকনে ক্লিক করলে যাতে রিস্টার্ট না হয়
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // ইনটেন্ট আপডেট
    }

    private void showControlCenter() {
        try {
            final Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.layout_control_center);

            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setGravity(Gravity.BOTTOM);
            }

            // ... (বাকি বাটন লজিক আগের মতই)
            // শর্টকাটের জন্য লজিক স্কিপ করলাম, আগের কোডই থাকবে এখানে

            dialog.show();
        } catch (Exception e) {}
    }

    // --- AUTO PiP (Home Button Fix) ---
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

    // --- PiP UI Handle ---
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            btnMenuTrigger.setVisibility(View.GONE);
            mOfflineLayout.setVisibility(View.GONE); // PiP তে অফলাইন পেজ হাইড
        } else {
            btnMenuTrigger.setVisibility(View.VISIBLE);
            // PiP থেকে ফিরলে অডিও মোড চেক
            if (myWebView.getUrl() == null) checkNetworkAndLoad();
        }
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
        // মিক্সড কন্টেন্ট এলাউ করা (অ্যাড ব্লকের জন্য জরুরি)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Mobile Safari/537.36");

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString().toLowerCase();
                
                // --- POWERFUL AD BLOCKER (Network Level) ---
                if (url.contains("googleads") || 
                    url.contains("doubleclick") || 
                    url.contains("adservice") || 
                    url.contains("googlesyndication") ||
                    url.contains("youtube.com/api/stats/ads") || // ভিডিও অ্যাড ট্র্যাকার
                    url.contains("ptracking")) {
                    
                    return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // পেজ লোড হলে স্ক্রিপ্ট ইনজেক্ট
                view.loadUrl(ScriptInjector.getRemoveAdsScript());
                super.onPageFinished(view, url);
            }
        });

        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) { request.grant(request.getResources()); }
            
            // Fullscreen Logic (Same as before)
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (mCustomView != null) { callback.onCustomViewHidden(); return; }
                mCustomView = view;
                myWebView.setVisibility(View.GONE);
                mFullscreenContainer.setVisibility(View.VISIBLE);
                mFullscreenContainer.addView(view);
                mCustomViewCallback = callback;
                btnMenuTrigger.setVisibility(View.GONE);
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
                btnMenuTrigger.setVisibility(View.VISIBLE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        });
    }

    // --- BACKGROUND PLAY FIX ---
    // আমরা ইচ্ছা করে WebView কে Pause হতে দিচ্ছি না
    @Override
    protected void onPause() {
        super.onPause(); // অ্যাক্টিভিটি পজ হবে
        // myWebView.onPause(); // এই লাইনটা ডিলিট করে দিয়েছি, তাই অডিও বন্ধ হবে না
    }

    // মেমরি লিক যাতে না হয়, তাই অ্যাপ পুরোপুরি বন্ধ করলেই কেবল ওয়েবভিউ ধ্বংস হবে
    @Override
    protected void onDestroy() {
        if (myWebView != null) {
            myWebView.destroy();
        }
        super.onDestroy();
    }
    
    // টোস্ট এবং ব্যাকপ্রেস লজিক আগের মতই...
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
            // হোম এ ব্যাক করবে কিন্তু অ্যাপ মারবে না
            moveTaskToBack(true);
        }
    }
}

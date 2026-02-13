package com.jubair.youtube;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;
    private FrameLayout mFullscreenContainer;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private ProgressBar progressBar;
    
    // ডাবল ট্যাপ টাইমার
    private long pressedTime;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myWebView = findViewById(R.id.main_webview);
        mFullscreenContainer = findViewById(R.id.fullscreen_container);
        progressBar = findViewById(R.id.progressBar);

        initWebView();
        showWelcomeDialog();
        showSenseiToast();
    }

    private void initWebView() {
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        
        // Android 4.4 Performance
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // User Agent: সাধারণ অ্যান্ড্রয়েড হিসেবে পরিচয় দিচ্ছি যাতে মিনি প্লেয়ার কাজ করে
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile; rv:109.0) Gecko/109.0 Firefox/109.0");

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // ১. যদি ইউটিউব বা গুগল লগইন হয়, অ্যাপের ভেতরেই থাকবে
                if (url.contains("youtube.com") || url.contains("youtu.be") || url.contains("accounts.google.com") || url.contains("google.com/accounts")) {
                    return false; // Load inside WebView
                }
                
                // ২. অন্য যেকোনো লিংক (ফেসবুক, উইকিপিডিয়া ইত্যাদি) ব্রাউজারে ওপেন হবে
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // CSS Injection (Ad & Banner Remove)
                injectCSS();
                super.onPageFinished(view, url);
            }
        });

        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }

            // --- ফুল স্ক্রিন এবং রোটেশন লজিক ---
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

                // ফুল স্ক্রিন মোড অন
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                
                // **** এখানে জোর করে ল্যান্ডস্কেপ করা হচ্ছে ****
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }

            @Override
            public void onHideCustomView() {
                if (mCustomView == null) return;

                myWebView.setVisibility(View.VISIBLE);
                mFullscreenContainer.setVisibility(View.GONE);
                mFullscreenContainer.removeView(mCustomView);
                mCustomView = null;
                mCustomViewCallback.onCustomViewHidden();

                // ফুল স্ক্রিন মোড অফ
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                
                // **** আবার পোর্ট্রেট মোডে ফেরত আনা হচ্ছে ****
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        });

        myWebView.loadUrl("https://m.youtube.com");
    }

    private void injectCSS() {
        // বিরক্তিকর এলিমেন্ট হাইড করার কোড
        String script = "javascript:(function() {" +
                "var style = document.createElement('style');" +
                "style.innerHTML = '" +
                ".mobile-topbar-header { display: none !important; }" +
                ".ytm-app-upsell { display: none !important; }" +
                "ytm-statement-banner-renderer { display: none !important; }" +
                ".ad-container { display: none !important; }" +
                "';" +
                "document.head.appendChild(style);" +
                "})()";
        myWebView.loadUrl(script);
    }

    private void showWelcomeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Jubair Sensei Build");
        builder.setMessage("Welcome to Light YouTube.\nRotation Fixed & External Links open in Chrome.");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.setNeutralButton("TELEGRAM", (dialog, which) -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Jubairsensei"));
            startActivity(browserIntent);
        });
        builder.show();
    }

    private void showSenseiToast() {
        try {
            TextView text = new TextView(this);
            text.setText("Jubair Sensei");
            text.setTextColor(Color.parseColor("#32CD32")); // Lime
            text.setTypeface(null, android.graphics.Typeface.BOLD);
            text.setTextSize(16);
            text.setPadding(40, 20, 40, 20);
            text.setBackgroundColor(Color.BLACK);

            Toast toast = new Toast(getApplicationContext());
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 150);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(text);
            toast.show();
        } catch (Exception e) {
            // Ignore
        }
    }

    @Override
    public void onBackPressed() {
        // ফুল স্ক্রিন থেকে ব্যাক করা
        if (mCustomView != null) {
            WebChromeClient wcc = myWebView.getWebChromeClient();
            if (wcc != null) {
                wcc.onHideCustomView(); // এটা অটোমেটিক রোটেশন ঠিক করবে
            }
            return;
        }

        if (myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            if (pressedTime + 2000 > System.currentTimeMillis()) {
                super.onBackPressed();
                finish();
            } else {
                Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT).show();
            }
            pressedTime = System.currentTimeMillis();
        }
    }
}

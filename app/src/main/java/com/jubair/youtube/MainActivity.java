package com.jubair.youtube;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
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
    
    // ডাবল ট্যাপ টু এক্সিট ভেরিয়েবল
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
        
        // Android 4.4 Optimization
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // User Agent Spoofing (PC Mode ভাব ধরবে যাতে অ্যাপ ওপেন করতে না বলে)
        // অথবা একটি ক্লিন Android User Agent
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("youtube.com") || url.contains("youtu.be")) {
                    return false; // অ্যাপের ভেতরেই লোড হবে
                }
                // বাইরের লিংক (যেমন টেলিগ্রাম) ব্রাউজারে ওপেন হবে
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // সেন্সেই স্পেশাল স্ক্রিপ্ট: অ্যাপ ব্যানার এবং অ্যাড রিমুভ
                injectCSS();
                super.onPageFinished(view, url);
            }
        });

        myWebView.setWebChromeClient(new WebChromeClient() {
            // প্রোগ্রেস বার লজিক
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }

            // ফুল স্ক্রিন ভিডিও লজিক
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
            }
        });

        myWebView.loadUrl("https://m.youtube.com");
    }

    // CSS ইনজেকশন মেথড
    private void injectCSS() {
        String script = "javascript:(function() {" +
                "var style = document.createElement('style');" +
                "style.innerHTML = '" +
                // টপ হেডার রিমুভ (Redundant bar)
                ".mobile-topbar-header { display: none !important; }" +
                // অ্যাপ ওপেন করার প্রম্পট রিমুভ
                ".ytm-app-upsell { display: none !important; }" +
                "ytm-statement-banner-renderer { display: none !important; }" +
                // কিছু বিজ্ঞাপন ব্লক করার চেষ্টা
                ".ad-container { display: none !important; }" +
                "';" +
                "document.head.appendChild(style);" +
                "})()";
        myWebView.loadUrl(script);
    }

    private void showWelcomeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Developer Info");
        builder.setMessage("This simplified YouTube Build is modified by Jubair Sensei.\n\nJoin Telegram for more?");
        builder.setCancelable(false);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setNeutralButton("TELEGRAM", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Jubairsensei"));
                startActivity(browserIntent);
            }
        });
        
        builder.show();
    }

    private void showSenseiToast() {
        try {
            TextView text = new TextView(this);
            text.setText("Jubair Sensei");
            text.setTextColor(Color.parseColor("#32CD32")); // Lime Green
            text.setTypeface(null, android.graphics.Typeface.BOLD);
            text.setTextSize(16);
            text.setPadding(40, 20, 40, 20);
            
            // কালো ব্যাকগ্রাউন্ড
            text.setBackgroundColor(Color.BLACK);

            Toast toast = new Toast(getApplicationContext());
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 150);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(text);
            toast.show();
        } catch (Exception e) {
            Toast.makeText(this, "Jubair Sensei", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        // যদি ভিডিও ফুলস্ক্রিনে থাকে
        if (mCustomView != null) {
            WebChromeClient wcc = myWebView.getWebChromeClient(); // এটা নাল হতে পারে, তাই সরাসরি কলব্যাক ইউজ করছি
             if (wcc != null) {
                 // এটা কাজ না করলে সরাসরি কলব্যাক
             }
             // উপরের লজিকে আমরা mCustomViewCallback সেভ করেছি
             if (mCustomViewCallback != null) mCustomViewCallback.onCustomViewHidden();
             
             // ম্যানুয়ালি হাইড
             myWebView.setVisibility(View.VISIBLE);
             mFullscreenContainer.setVisibility(View.GONE);
             mFullscreenContainer.removeView(mCustomView);
             mCustomView = null;
             getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
             return;
        }

        // যদি ব্রাউজারে ব্যাক করার অপশন থাকে
        if (myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            // ডাবল ট্যাপ টু এক্সিট
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

package com.jubair.youtube;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// Import our custom classes
import com.jubair.youtube.utils.ScriptInjector;
import com.jubair.youtube.ui.DialogManager;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;
    private FrameLayout mFullscreenContainer;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private long pressedTime;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myWebView = findViewById(R.id.main_webview);
        mFullscreenContainer = findViewById(R.id.fullscreen_container);

        initWebView();
        
        // Show Cyberpunk Dialog
        DialogManager.showCyberpunkDialog(this);
        showSenseiToast();
    }

    private void initWebView() {
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        
        // Performance
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // User Agent: Chrome Mobile (Standard) ব্যবহার করছি "Sign in" এরর কমানোর জন্য
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36");

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // BLOCK intent:// and market:// links
                if (url.startsWith("intent://") || url.startsWith("market://") || url.startsWith("vnd.youtube:")) {
                    return true; // ব্লক করা হলো (কিছুই হবে না)
                }

                if (url.contains("youtube.com") || url.contains("youtu.be") || url.contains("accounts.google.com")) {
                    return false; // অ্যাপেই লোড হবে
                }
                
                // এক্সটারনাল লিংক (Chrome এ যাবে)
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
                // ইনজেক্ট CSS
                view.loadUrl(ScriptInjector.getRemoveAdsScript());
                super.onPageFinished(view, url);
            }
        });

        myWebView.setWebChromeClient(new WebChromeClient() {
            // Fullscreen Logic
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
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        });

        myWebView.loadUrl("https://m.youtube.com");
    }

    private void showSenseiToast() {
        try {
            TextView text = new TextView(this);
            text.setText("System: Jubair Sensei");
            text.setTextColor(Color.parseColor("#00FF00")); // Neon Green
            text.setTypeface(null, android.graphics.Typeface.BOLD);
            text.setTextSize(14);
            text.setPadding(30, 15, 30, 15);
            text.setBackgroundResource(R.drawable.neon_bg); // Using neon bg

            Toast toast = new Toast(getApplicationContext());
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(text);
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
            if (pressedTime + 2000 > System.currentTimeMillis()) {
                super.onBackPressed();
            } else {
                Toast.makeText(getBaseContext(), "Tap again to exit system", Toast.LENGTH_SHORT).show();
            }
            pressedTime = System.currentTimeMillis();
        }
    }
}

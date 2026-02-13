package com.jubair.youtube;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.jubair.youtube.utils.ScriptInjector;
import com.jubair.youtube.ui.DialogManager;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;
    private FrameLayout mFullscreenContainer;
    private RelativeLayout mOfflineLayout;
    private DrawerLayout mDrawerLayout;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    
    // ফিচার ফ্ল্যাগ
    private boolean isAudioMode = false;
    private long pressedTime;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ভিউ বাইন্ডিং
        myWebView = findViewById(R.id.main_webview);
        mFullscreenContainer = findViewById(R.id.fullscreen_container);
        mOfflineLayout = findViewById(R.id.offline_layout);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        
        Button btnRetry = findViewById(R.id.btn_retry);
        Button btnMenu = findViewById(R.id.btn_menu_trigger);
        Button btnAudio = findViewById(R.id.btn_audio_mode);
        Button btnExternal = findViewById(R.id.btn_external_play);
        Button btnReload = findViewById(R.id.btn_reload);

        initWebView();
        
        // ডায়লগ সাইজ ফিক্স (Full Width)
        DialogManager.showCyberpunkDialog(this);
        showSenseiToast();

        // --- বাটন লজিক ---
        
        // ১. মেনু ওপেনার
        btnMenu.setOnClickListener(v -> mDrawerLayout.openDrawer(Gravity.LEFT));

        // ২. অডিও মোড (Background Play)
        btnAudio.setOnClickListener(v -> {
            isAudioMode = !isAudioMode;
            if(isAudioMode) {
                btnAudio.setText("Audio Mode: ON");
                btnAudio.setTextColor(Color.GREEN);
                Toast.makeText(this, "Background Play Enabled", Toast.LENGTH_SHORT).show();
            } else {
                btnAudio.setText("Audio Mode: OFF");
                btnAudio.setTextColor(Color.WHITE);
            }
            mDrawerLayout.closeDrawers();
        });

        // ৩. এক্সটারনাল প্লেয়ার (Default Player)
        btnExternal.setOnClickListener(v -> {
            String currentUrl = myWebView.getUrl();
            if(currentUrl != null) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(currentUrl), "video/*");
                    startActivity(Intent.createChooser(intent, "Open with..."));
                } catch (Exception e) {
                    Toast.makeText(this, "No External Player Found", Toast.LENGTH_SHORT).show();
                }
            }
            mDrawerLayout.closeDrawers();
        });

        // ৪. রিলোড
        btnReload.setOnClickListener(v -> {
            myWebView.reload();
            mDrawerLayout.closeDrawers();
        });
        
        // ৫. রিট্রাই (অফলাইন)
        btnRetry.setOnClickListener(v -> checkNetworkAndLoad());
        
        // প্রথমে চেক করো নেট আছে কিনা
        checkNetworkAndLoad();
    }

    private void checkNetworkAndLoad() {
        if (isNetworkAvailable()) {
            mOfflineLayout.setVisibility(View.GONE);
            myWebView.setVisibility(View.VISIBLE);
            if (myWebView.getUrl() == null) {
                myWebView.loadUrl("https://m.youtube.com");
            } else {
                myWebView.reload();
            }
        } else {
            myWebView.setVisibility(View.GONE);
            mOfflineLayout.setVisibility(View.VISIBLE);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void initWebView() {
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false); // অটোপ্লে এবং অডিও এর জন্য জরুরি
        
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36");

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl(ScriptInjector.getRemoveAdsScript());
                super.onPageFinished(view, url);
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // নেট চলে গেলে অটোমেটিক অফলাইন পেজ
                myWebView.setVisibility(View.GONE);
                mOfflineLayout.setVisibility(View.VISIBLE);
            }
        });

        myWebView.setWebChromeClient(new WebChromeClient() {
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
    }

    // --- Background Audio Logic ---
    // যখন Audio Mode অন থাকবে, তখন আমরা সুপার ক্লাসের onPause কল করব না
    @Override
    protected void onPause() {
        if (isAudioMode) {
            // Do NOT call myWebView.onPause()
            super.onPause(); 
        } else {
            myWebView.onPause();
            super.onPause();
        }
    }

    @Override
    protected void onResume() {
        myWebView.onResume();
        super.onResume();
    }

    private void showSenseiToast() {
        // (আগের টোস্ট কোড অপরিবর্তিত)
        try {
            TextView text = new TextView(this);
            text.setText("System: Jubair Sensei");
            text.setTextColor(Color.parseColor("#00FF00"));
            text.setTypeface(null, android.graphics.Typeface.BOLD);
            text.setTextSize(14);
            text.setPadding(30, 15, 30, 15);
            text.setBackgroundResource(R.drawable.neon_bg);

            Toast toast = new Toast(getApplicationContext());
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(text);
            toast.show();
        } catch (Exception e) {}
    }

    @Override
    public void onBackPressed() {
        // ড্রয়ার খোলা থাকলে বন্ধ করো
        if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
            mDrawerLayout.closeDrawer(Gravity.LEFT);
            return;
        }
        // ফুলস্ক্রিন থাকলে ছোট করো
        if (mCustomView != null) {
            WebChromeClient wcc = myWebView.getWebChromeClient();
            if (wcc != null) wcc.onHideCustomView();
            return;
        }
        // ব্রাউজার ব্যাক
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

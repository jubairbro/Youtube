package com.jubair.youtube;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.jubair.youtube.utils.ScriptInjector;
import com.jubair.youtube.ui.DialogManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;
    private FrameLayout mFullscreenContainer;
    private RelativeLayout mOfflineLayout;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    
    // UI Controls
    private Button btnMenuTrigger; 
    
    private boolean isAudioMode = true; // ডিফল্টভাবে অন থাকবে (সেন্সেই মোড)
    private long pressedTime;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myWebView = findViewById(R.id.main_webview);
        mFullscreenContainer = findViewById(R.id.fullscreen_container);
        mOfflineLayout = findViewById(R.id.offline_layout);
        btnMenuTrigger = findViewById(R.id.btn_menu_trigger);
        Button btnRetry = findViewById(R.id.btn_retry);

        initWebView();
        DialogManager.showCyberpunkDialog(this);
        showSenseiToast();

        // মেনু বাটন লজিক (Bottom Sheet ওপেন হবে)
        btnMenuTrigger.setOnClickListener(v -> showControlCenter());

        btnRetry.setOnClickListener(v -> checkNetworkAndLoad());
        checkNetworkAndLoad();
    }

    // --- NEW: CONTROL CENTER (Bottom Sheet) ---
    // এটা ক্র্যাশ করবে না কারণ এটা অ্যাক্টিভিটির পার্ট
    private void showControlCenter() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.layout_control_center);
        
        // ব্যাকগ্রাউন্ড ট্রান্সপারেন্ট (রাউন্ড শেপ দেখার জন্য)
        if (bottomSheetDialog.getWindow() != null) {
            bottomSheetDialog.getWindow().findViewById(com.google.android.material.R.id.design_bottom_sheet).setBackgroundResource(android.R.color.transparent);
        }

        Switch switchAudio = bottomSheetDialog.findViewById(R.id.switch_audio);
        LinearLayout btnPip = bottomSheetDialog.findViewById(R.id.btn_pip);
        LinearLayout btnReload = bottomSheetDialog.findViewById(R.id.btn_reload);
        LinearLayout btnExit = bottomSheetDialog.findViewById(R.id.btn_exit);

        // Audio Logic
        switchAudio.setChecked(isAudioMode);
        switchAudio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAudioMode = isChecked;
            Toast.makeText(this, isAudioMode ? "Background Play: ON" : "Background Play: OFF", Toast.LENGTH_SHORT).show();
        });

        // PiP Logic
        btnPip.setOnClickListener(v -> {
            enterPiPMode();
            bottomSheetDialog.dismiss();
        });

        // Reload Logic
        btnReload.setOnClickListener(v -> {
            myWebView.reload();
            bottomSheetDialog.dismiss();
        });
        
        // Exit
        btnExit.setOnClickListener(v -> {
            finishAffinity(); // একদম সব ক্লোজ করে বের হয়ে যাবে
        });

        bottomSheetDialog.show();
    }

    private void enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Rational aspectRatio = new Rational(16, 9);
            PictureInPictureParams.Builder params = new PictureInPictureParams.Builder();
            params.setAspectRatio(aspectRatio);
            enterPictureInPictureMode(params.build());
        } else {
            Toast.makeText(this, "PiP not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            btnMenuTrigger.setVisibility(View.GONE);
        } else {
            btnMenuTrigger.setVisibility(View.VISIBLE);
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

    private void initWebView() {
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false); // অটোপ্লে এবং ব্যাকগ্রাউন্ড প্লে ফিক্স
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36");

        myWebView.setWebViewClient(new WebViewClient() {
            // --- NETWORK LEVEL AD BLOCKING ---
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString().toLowerCase();
                // অ্যাড সার্ভার ডোমেইন ব্লক করা হচ্ছে
                if (url.contains("googleads") || 
                    url.contains("doubleclick") || 
                    url.contains("adservice") || 
                    url.contains("googlesyndication")) {
                    // অ্যাড রিকোয়েস্ট পেলে খালি রেসপন্স রিটার্ন করবে (ব্লক)
                    InputStream empty = new ByteArrayInputStream("".getBytes());
                    return new WebResourceResponse("text/plain", "utf-8", empty);
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl(ScriptInjector.getRemoveAdsScript());
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
                btnMenuTrigger.setVisibility(View.GONE); // হাইড বাটন
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
                btnMenuTrigger.setVisibility(View.VISIBLE); // শো বাটন
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        });
    }

    // --- BACKGROUND PLAY LOGIC ---
    @Override
    protected void onPause() {
        // যদি অডিও মোড অন থাকে, তাহলে আমরা WebView কে Pause হতে দিব না
        if (isAudioMode) {
            super.onPause(); // শুধু অ্যাক্টিভিটি পজ হবে
            // myWebView.onPause() কল করা যাবে না!
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
            super.onBackPressed();
        }
    }
}

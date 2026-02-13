package com.jubair.youtube;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.jubair.youtube.utils.ScriptInjector;
import com.jubair.youtube.ui.DialogManager;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;
    private FrameLayout mFullscreenContainer;
    private RelativeLayout mOfflineLayout;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private ImageView btnModMenu;
    
    private boolean isAudioMode = false;
    private long pressedTime;

    // Draggable Variables
    private float dX, dY;
    private float startX, startY;
    private static final int CLICK_ACTION_THRESHOLD = 10;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myWebView = findViewById(R.id.main_webview);
        mFullscreenContainer = findViewById(R.id.fullscreen_container);
        mOfflineLayout = findViewById(R.id.offline_layout);
        btnModMenu = findViewById(R.id.btn_mod_menu);
        Button btnRetry = findViewById(R.id.btn_retry);

        initWebView();
        DialogManager.showCyberpunkDialog(this);
        showSenseiToast();

        // --- DRAGGABLE MOD MENU LOGIC ---
        btnModMenu.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        startX = event.getRawX();
                        startY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        view.animate()
                            .x(event.getRawX() + dX)
                            .y(event.getRawY() + dY)
                            .setDuration(0)
                            .start();
                        return true;

                    case MotionEvent.ACTION_UP:
                        float endX = event.getRawX();
                        float endY = event.getRawY();
                        if (Math.abs(startX - endX) < CLICK_ACTION_THRESHOLD && 
                            Math.abs(startY - endY) < CLICK_ACTION_THRESHOLD) {
                            // যদি বেশি না নড়ে, তাহলে এটা ক্লিক
                            showModMenuDialog();
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });

        btnRetry.setOnClickListener(v -> checkNetworkAndLoad());
        checkNetworkAndLoad();
    }

    private void showModMenuDialog() {
        try {
            final Dialog dialog = new Dialog(MainActivity.this); // Context fix
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_mod_menu);
            
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            Button btnPip = dialog.findViewById(R.id.btn_pip);
            Switch switchAudio = dialog.findViewById(R.id.switch_audio);
            Button btnExt = dialog.findViewById(R.id.btn_mod_external);
            Button btnReload = dialog.findViewById(R.id.btn_mod_reload);
            TextView btnClose = dialog.findViewById(R.id.btn_close_menu);

            // PiP Logic
            btnPip.setOnClickListener(v -> {
                enterPiPMode();
                dialog.dismiss();
            });

            switchAudio.setChecked(isAudioMode);
            switchAudio.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isAudioMode = isChecked;
                Toast.makeText(MainActivity.this, isChecked ? "Audio Mode: ON" : "Audio Mode: OFF", Toast.LENGTH_SHORT).show();
            });

            btnExt.setOnClickListener(v -> {
                String currentUrl = myWebView.getUrl();
                if(currentUrl != null) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(currentUrl), "video/*");
                        startActivity(Intent.createChooser(intent, "Open with..."));
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "No External Player", Toast.LENGTH_SHORT).show();
                    }
                }
                dialog.dismiss();
            });

            btnReload.setOnClickListener(v -> {
                myWebView.reload();
                dialog.dismiss();
            });

            btnClose.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(this, "Error opening menu", Toast.LENGTH_SHORT).show();
        }
    }

    private void enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Rational aspectRatio = new Rational(16, 9);
            PictureInPictureParams.Builder params = new PictureInPictureParams.Builder();
            params.setAspectRatio(aspectRatio);
            enterPictureInPictureMode(params.build());
        } else {
            Toast.makeText(this, "PiP not supported on this Android version", Toast.LENGTH_SHORT).show();
        }
    }

    // PiP মোডে UI হাইড করা
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            btnModMenu.setVisibility(View.GONE);
        } else {
            btnModMenu.setVisibility(View.VISIBLE);
        }
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
        webSettings.setMediaPlaybackRequiresUserGesture(false);
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
                myWebView.setVisibility(View.GONE);
                mOfflineLayout.setVisibility(View.VISIBLE);
            }
        });

        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }

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
                
                btnModMenu.setVisibility(View.GONE);
                
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
                
                btnModMenu.setVisibility(View.VISIBLE);

                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        });
    }

    @Override
    protected void onPause() {
        if (isAudioMode || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode())) {
            super.onPause(); 
        } else {
            myWebView.onPause();
            super.onPause();
        }
    }

    private void showSenseiToast() {
        try {
            TextView text = new TextView(this);
            text.setText("System: Jubair Sensei");
            text.setTextColor(Color.parseColor("#00FF00"));
            text.setTypeface(null, android.graphics.Typeface.BOLD);
            text.setTextSize(14);
            text.setPadding(30, 15, 30, 15);
            text.setBackgroundResource(R.drawable.neon_bg);
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
            if (pressedTime + 2000 > System.currentTimeMillis()) {
                super.onBackPressed();
            } else {
                Toast.makeText(getBaseContext(), "Tap again to EXIT", Toast.LENGTH_SHORT).show();
            }
            pressedTime = System.currentTimeMillis();
        }
    }
}

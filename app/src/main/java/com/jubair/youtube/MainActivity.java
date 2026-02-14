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

    // ১. মিডিয়া কন্ট্রোল রিসিভার (নোটিফিকেশন বার থেকে প্লে/পজ করার জন্য)
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
        
        // ২. ক্র্যাশ হ্যান্ডলার সেটআপ
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
        
        // ৩. মিডিয়া রিসিভার রেজিস্টার করা
        IntentFilter filter = new IntentFilter();
        filter.addAction("ACTION_PLAY");
        filter.addAction("ACTION_PAUSE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(mediaReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mediaReceiver, filter);
        }
        
        // ৪. ব্যাকগ্রাউন্ড অডিও সার্ভিস চালু করা
        startService(new Intent(this, BackgroundAudioService.class));
        
        // ৫. স্ক্রিন অন রাখা (ভিডিও দেখার সময় ডিম হবে না)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        // ভিউ বাইন্ডিং
        myWebView = findViewById(R.id.main_webview);
        mFullscreenContainer = findViewById(R.id.fullscreen_container);
        mOfflineLayout = findViewById(R.id.offline_layout);
        Button btnRetry = findViewById(R.id.btn_retry);

        initWebView();
        
        // ৬. টোস্ট এবং ওয়েলকাম ডায়লগ (শুধু অ্যাপ স্টার্টের সময়)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isInPictureInPictureMode()) {
            Toast.makeText(this, "Jubair Sensei", Toast.LENGTH_SHORT).show();
            DialogManager.showWelcomeDialog(this);
        }

        // রিট্রাই বাটন লজিক
        btnRetry.setOnClickListener(v -> checkNetworkAndLoad());
        checkNetworkAndLoad();
    }

    private void initWebView() {
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); // সেটিংস সেভ রাখার জন্য
        webSettings.setDatabaseEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false); // অডিও ব্যাকগ্রাউন্ডের জন্য জরুরি
        
        // মিক্সড কন্টেন্ট (SSL) হ্যান্ডলিং
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        
        // ৭. ইউজার এজেন্ট (মডার্ন ব্রাউজার হিসেবে পরিচয় দেওয়া)
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // ৮. অ্যাড ব্লকার: নেটওয়ার্ক লেভেলে অ্যাড ব্লক করা
                if (AdBlocker.isAd(request.getUrl().toString())) {
                    return AdBlocker.createEmptyResponse();
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // ৯. স্ক্রিপ্ট ইনজেকশন (JSON Hijack + UI Clean + Auto Skip)
                view.loadUrl(ScriptInjector.getInjectScript());
                super.onPageFinished(view, url);
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // নেটওয়ার্ক এরর হ্যান্ডলিং
                myWebView.setVisibility(View.GONE);
                mOfflineLayout.setVisibility(View.VISIBLE);
            }
        });

        myWebView.setWebChromeClient(new WebChromeClient() {
            // ১০. পারমিশন অটো অ্যালাউ
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }

            // ১১. ফুলস্ক্রিন ভিডিও লজিক
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
                
                // ফুলস্ক্রিন অন
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
                
                // পোর্ট্রেট মোডে ফেরত
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        });

        // ডিফল্ট লোড
        if (myWebView.getUrl() == null) {
            myWebView.loadUrl("https://m.youtube.com");
        }
    }

    // ১২. নেটওয়ার্ক চেকিং
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

    // ১৩. ব্যাকগ্রাউন্ড প্লে লজিক (সবচেয়ে গুরুত্বপূর্ণ)
    @Override
    protected void onPause() {
        super.onPause();
        // WebView Pause হতে দিচ্ছি না, যাতে অ্যাপ মিনিমাইজ করলেও গান চলে
    }

    @Override
    protected void onDestroy() {
        // অ্যাপ পুরোপুরি কিল করলে রিসিভার এবং সার্ভিস স্টপ হবে
        try {
            unregisterReceiver(mediaReceiver);
        } catch (Exception e) {}
        stopService(new Intent(this, BackgroundAudioService.class));
        super.onDestroy();
    }

    // ১৪. অটো PiP (হোম বাটন চাপলে)
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
                // PiP সাপোর্ট না করলে ইগনোর করবে
            }
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            mOfflineLayout.setVisibility(View.GONE);
            // PiP মোডে এক্সট্রা UI হাইড হবে ScriptInjector এর CSS দিয়ে
        }
    }

    // ১৫. ব্যাক বাটন হ্যান্ডেলিং
    @Override
    public void onBackPressed() {
        if (mCustomView != null) {
            // ফুলস্ক্রিন থেকে বের হওয়া
            WebChromeClient wcc = myWebView.getWebChromeClient();
            if (wcc != null) wcc.onHideCustomView();
            return;
        }
        if (myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            // হোম এ ব্যাক করা (অ্যাপ ব্যাকগ্রাউন্ডে থাকবে)
            moveTaskToBack(true);
        }
    }
}

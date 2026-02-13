package com.jubair.youtube;

import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.jubair.youtube.utils.ScriptInjector;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myWebView = findViewById(R.id.main_webview);
        initWebView();
    }

    private void initWebView() {
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); // GoodTube সেটিংস সেভ রাখার জন্য জরুরি
        webSettings.setMediaPlaybackRequiresUserGesture(false); // অটোপ্লে এর জন্য
        
        // Desktop Mode বা Brave এর মত UserAgent
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // পেজ লোড শেষ হলে GoodTube স্ক্রিপ্ট ইনজেক্ট হবে
                view.loadUrl(ScriptInjector.getGoodTubeScript());
                super.onPageFinished(view, url);
            }
        });

        myWebView.setWebChromeClient(new WebChromeClient());
        
        // ডিফল্ট ইউটিউব লোড
        if (myWebView.getUrl() == null) {
            myWebView.loadUrl("https://m.youtube.com");
        }
    }

    // --- ব্যাকগ্রাউন্ড প্লে ফিক্স ---
    @Override
    protected void onPause() {
        super.onPause();
        // myWebView.onPause(); // এটা বন্ধ রাখলে ব্যাকগ্রাউন্ডে অডিও চলবে
    }

    // --- PiP মোড হ্যান্ডেল ---
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
                // PiP সাপোর্ট না করলে কিছু হবে না
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

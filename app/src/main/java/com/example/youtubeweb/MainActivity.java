package com.example.youtubeweb;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private Handler handler = new Handler();
    private Runnable toastRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        toastRunnable = new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(
                        getApplicationContext(),
                        "Jubair Ahmad",
                        Toast.LENGTH_SHORT
                );
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(toastRunnable);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        settings.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 4.4.2) AppleWebKit/537.36 Chrome/86.0 Mobile Safari/537.36"
        );

        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("https://m.youtube.com");
    }
}

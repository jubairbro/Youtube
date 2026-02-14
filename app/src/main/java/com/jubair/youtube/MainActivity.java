package com.jubair.youtube;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.jubair.youtube.managers.AdBlocker;
import com.jubair.youtube.services.BackgroundAudioService;
import com.jubair.youtube.utils.GoodTubeScript;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;
    // Encoded UA
    private final String UA = new String(Base64.decode("TW96aWxsYS81LjAgKExpbnV4OyBBbmRyb2lkIDEzOyBQaXhlbCA3KSBBcHBsZVdlYktpdC81MzcuMzYgKEtIVE1MLCBsaWtlIEdlY2tvKSBDaHJvbWUvMTE2LjAuMC4wIE1vYmlsIFNhZmFyaS81MzcuMzY=", Base64.DEFAULT));

    public class WebAppInterface {
        @JavascriptInterface
        public void onVideoPlay() { toggleService(true); }
        @JavascriptInterface
        public void onVideoPause() { toggleService(false); }
    }

    private void toggleService(boolean play) {
        Intent i = new Intent(this, BackgroundAudioService.class);
        i.putExtra("IS_PLAYING", play);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
        else startService(i);
    }

    private final BroadcastReceiver mediaReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String cmd = intent.getStringExtra("COMMAND");
            if (cmd != null) {
                if (cmd.equals("PLAY")) myWebView.evaluateJavascript("document.querySelector('video').play();", null);
                else if (cmd.equals("PAUSE")) myWebView.evaluateJavascript("document.querySelector('video').pause();", null);
                else if (cmd.equals("NEXT")) myWebView.evaluateJavascript("document.querySelector('.ytp-next-button').click();", null);
                else if (cmd.equals("PREV")) myWebView.evaluateJavascript("document.querySelector('.ytp-prev-button').click();", null);
            }
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        IntentFilter f = new IntentFilter("MEDIA_CONTROL");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) registerReceiver(mediaReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(mediaReceiver, f);

        myWebView = findViewById(R.id.main_webview);
        initWebView();
    }

    private void initWebView() {
        WebSettings s = myWebView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString(UA);

        myWebView.addJavascriptInterface(new WebAppInterface(), "Android");
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl(GoodTubeScript.getInjectScript());
            }
        });
        
        myWebView.setWebChromeClient(new WebChromeClient());
        myWebView.loadUrl("https://m.youtube.com");
    }

    @Override
    public void onBackPressed() {
        if (myWebView.canGoBack()) myWebView.goBack();
        else moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mediaReceiver);
        stopService(new Intent(this, BackgroundAudioService.class));
        super.onDestroy();
    }
}

package com.jubair.youtube;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // WebView Setup
        myWebView = new WebView(this);
        setContentView(myWebView);

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        
        // Android 4.4 Performance Fixes
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        myWebView.setWebViewClient(new WebViewClient());
        myWebView.loadUrl("https://m.youtube.com");

        // Custom Colorful Toast
        showCustomToast();
    }

    private void showCustomToast() {
        try {
            // Using a simple layout created programmatically to avoid complex XML for just a toast
            TextView text = new TextView(this);
            text.setText("App by \"Jubair Ahmad\"");
            text.setTextColor(Color.WHITE);
            text.setTextSize(16);
            text.setPadding(30, 20, 30, 20);
            text.setBackgroundColor(Color.parseColor("#CCFF0000")); // Semi-transparent Red
            text.setGravity(Gravity.CENTER);
            
            // Corner radius styling requires a drawable, simplified here for compatibility:
            // Just a bold colored box as requested.

            Toast toast = new Toast(getApplicationContext());
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(text);
            toast.show();
        } catch (Exception e) {
            // Fallback for very old devices if custom toast fails
            Toast.makeText(this, "App by Jubair Ahmad", Toast.LENGTH_LONG).show();
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

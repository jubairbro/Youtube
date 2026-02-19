package com.sensei.youtube.ui

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sensei.youtube.R
import com.sensei.youtube.databinding.ActivityMainBinding
import com.sensei.youtube.databinding.DialogTelegramBinding
import com.sensei.youtube.utils.AdBlocker
import com.sensei.youtube.utils.PreferenceManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var telegramDialogShown = false
    private var customViewContainer: FrameLayout? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var isFullscreen = false
    
    companion object {
        private const val TAG = "MainActivity"
        private const val YOUTUBE_URL = "https://m.youtube.com"
        private const val YOUTUBE_DESKTOP_URL = "https://www.youtube.com"
        
        private const val MOBILE_UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        private const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupWebView()
        setupNetworkCallback()
        setupBackPressedHandler()
        setupFab()
        checkAndShowTelegramDialog()
    }
    
    @Suppress("DEPRECATION", "SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.mainWebview
        
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            blockNetworkImage = false
            loadsImagesAutomatically = true
            userAgentString = if (PreferenceManager.isDesktopMode) DESKTOP_UA else MOBILE_UA
        }
        
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
            flush()
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url.toString()
                
                if (url.contains(".jpg") || url.contains(".png") || 
                    url.contains(".webp") || url.contains(".gif") ||
                    url.contains("ytimg.com") || url.contains("ggpht.com") ||
                    url.contains("googleusercontent.com")) {
                    return super.shouldInterceptRequest(view, request)
                }
                
                if (PreferenceManager.isAdBlockEnabled && AdBlocker.isAd(url)) {
                    return AdBlocker.createEmptyResponse()
                }
                return super.shouldInterceptRequest(view, request)
            }
            
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.visibility = View.VISIBLE
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = View.GONE
                injectScripts()
            }
            
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                if (!isNetworkAvailable()) {
                    showOfflineView()
                }
            }
        }
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progressBar.visibility = if (newProgress == 100) View.GONE else View.VISIBLE
            }
            
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customViewContainer != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                
                isFullscreen = true
                customViewCallback = callback
                customViewContainer = FrameLayout(this@MainActivity).apply {
                    setBackgroundColor(0xFF000000.toInt())
                    addView(view, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    ))
                }
                
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                setContentView(customViewContainer)
            }
            
            override fun onHideCustomView() {
                if (customViewContainer == null) return
                
                isFullscreen = false
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                
                customViewContainer?.removeAllViews()
                customViewContainer = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                
                setContentView(binding.root)
            }
            
            override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request?.grant(request.resources)
                }
            }
        }
        
        loadUrl()
    }
    
    private fun loadUrl() {
        val url = if (PreferenceManager.isDesktopMode) YOUTUBE_DESKTOP_URL else YOUTUBE_URL
        binding.mainWebview.loadUrl(url)
    }
    
    private fun injectScripts() {
        val js = """
            (function() {
                'use strict';
                
                // Ad blocking
                if (window.__adBlockInjected === undefined) {
                    window.__adBlockInjected = true;
                    var style = document.createElement('style');
                    style.innerHTML = `
                        .ytp-ad-module, .ytp-ad-overlay-container, .video-ads, 
                        .ytp-ad-progress-list, ytd-ad-slot-renderer, ytd-display-ad-renderer,
                        ytd-promoted-video-renderer, ytd-in-feed-ad-layout-renderer,
                        #masthead-ad, #player-ads, .ytp-ad-player-overlay,
                        tp-yt-iron-overlay-backdrop, ytd-mealbar-promo-renderer {
                            display: none !important; visibility: hidden !important;
                        }
                    `;
                    document.head.appendChild(style);
                }
                
                // Background play - Override visibility
                if (window.__bgPlayInjected === undefined) {
                    window.__bgPlayInjected = true;
                    
                    try {
                        Object.defineProperty(document, 'hidden', {
                            get: function() { return false; },
                            configurable: true
                        });
                        Object.defineProperty(document, 'visibilityState', {
                            get: function() { return 'visible'; },
                            configurable: true
                        });
                        Object.defineProperty(document, 'webkitHidden', {
                            get: function() { return false; },
                            configurable: true
                        });
                    } catch(e) {}
                    
                    var blockEvent = function(e) {
                        e.stopImmediatePropagation();
                        e.preventDefault();
                    };
                    
                    ['visibilitychange', 'webkitvisibilitychange', 'pagehide', 'freeze'].forEach(function(evt) {
                        document.addEventListener(evt, blockEvent, true);
                        window.addEventListener(evt, blockEvent, true);
                    });
                    
                    // Prevent auto-pause
                    var origPause = HTMLVideoElement.prototype.pause;
                    HTMLVideoElement.prototype.pause = function() {
                        if (this.__userPaused || this.ended) {
                            return origPause.apply(this, arguments);
                        }
                    };
                    
                    // Track user pause
                    document.addEventListener('click', function(e) {
                        var btn = e.target.closest('.ytp-play-button');
                        if (btn) {
                            var v = document.querySelector('video');
                            if (v) v.__userPaused = !v.paused;
                        }
                    }, true);
                    
                    // Keep playing
                    setInterval(function() {
                        var v = document.querySelector('video');
                        if (v && !v.paused === false && !v.ended && !v.__userPaused) {
                            v.play().catch(function(){});
                        }
                    }, 500);
                }
            })();
        """.trimIndent()
        
        binding.mainWebview.evaluateJavascript(js, null)
    }
    
    private fun setupNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread { hideOfflineView() }
            }
            override fun onLost(network: Network) {
                runOnUiThread { showOfflineView() }
            }
        }
        cm.registerNetworkCallback(request, networkCallback!!)
    }
    
    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    private fun showOfflineView() {
        binding.offlineLayout.visibility = View.VISIBLE
        binding.mainWebview.visibility = View.GONE
        binding.btnRetry.setOnClickListener {
            if (isNetworkAvailable()) {
                hideOfflineView()
                binding.mainWebview.reload()
            }
        }
    }
    
    private fun hideOfflineView() {
        binding.offlineLayout.visibility = View.GONE
        binding.mainWebview.visibility = View.VISIBLE
    }
    
    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFullscreen && customViewCallback != null) {
                    binding.mainWebview.webChromeClient?.onHideCustomView()
                    return
                }
                
                if (binding.mainWebview.canGoBack()) {
                    binding.mainWebview.goBack()
                } else {
                    moveTaskToBack(true)
                }
            }
        })
    }
    
    private fun setupFab() {
        binding.fabPip.setOnClickListener { enterPiP() }
    }
    
    private fun enterPiP(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                return try {
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                setAutoEnterEnabled(true)
                            }
                        }
                        .build()
                    enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    Log.e(TAG, "PiP failed", e)
                    false
                }
            }
        }
        return false
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && PreferenceManager.isBackgroundPlayEnabled) {
            enterPiP()
        }
    }
    
    override fun onPictureInPictureModeChanged(isInPiP: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPiP, newConfig)
        binding.fabPip.isVisible = !isInPiP
        
        if (isInPiP) {
            binding.mainWebview.evaluateJavascript(
                "(function(){var v=document.querySelector('video');if(v)v.play();})();",
                null
            )
        }
    }
    
    private fun checkAndShowTelegramDialog() {
        if (!PreferenceManager.isTgJoined && !telegramDialogShown) {
            showTelegramDialog()
            telegramDialogShown = true
        }
    }
    
    private fun showTelegramDialog() {
        val dialogBinding = DialogTelegramBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialogBinding.btnJoin.setOnClickListener {
            PreferenceManager.isTgJoined = true
            openTelegram()
            dialog.dismiss()
        }
        
        dialogBinding.btnLater.setOnClickListener {
            PreferenceManager.isTgJoined = true
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun openTelegram() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=JubairSensei")))
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/JubairSensei")))
        }
    }
    
    fun hardReload() {
        CookieManager.getInstance().flush()
        binding.mainWebview.clearCache(true)
        binding.mainWebview.clearHistory()
        loadUrl()
        Toast.makeText(this, "Reloaded", Toast.LENGTH_SHORT).show()
    }
    
    fun clearAllCache() {
        binding.mainWebview.clearCache(true)
        binding.mainWebview.clearHistory()
        CookieManager.getInstance().removeAllCookies(null)
        cacheDir.deleteRecursively()
        Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
    }
    
    fun toggleDesktopMode(enabled: Boolean) {
        PreferenceManager.isDesktopMode = enabled
        binding.mainWebview.settings.userAgentString = if (enabled) DESKTOP_UA else MOBILE_UA
        hardReload()
    }
    
    override fun onResume() {
        super.onResume()
        binding.mainWebview.onResume()
        injectScripts()
    }
    
    override fun onPause() {
        super.onPause()
        // Don't pause WebView for background play
        binding.mainWebview.evaluateJavascript(
            "(function(){var v=document.querySelector('video');if(v&&!v.ended)v.play();})();",
            null
        )
    }
    
    override fun onDestroy() {
        networkCallback?.let {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(it)
        }
        super.onDestroy()
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { binding.mainWebview.loadUrl(it.toString()) }
    }
}

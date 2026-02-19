package com.sensei.youtube.ui

import android.annotation.SuppressLint
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
import android.os.PowerManager
import android.provider.Settings
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sensei.youtube.R
import com.sensei.youtube.databinding.ActivityMainBinding
import com.sensei.youtube.databinding.DialogTelegramBinding
import com.sensei.youtube.services.MusicPlayerService
import com.sensei.youtube.utils.AdBlocker
import com.sensei.youtube.utils.PreferenceManager
import com.sensei.youtube.utils.WebAppInterface

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var telegramDialogShown = false
    private var customViewContainer: FrameLayout? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    private val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    
    companion object {
        private const val TAG = "MainActivity"
        private const val YOUTUBE_URL = "https://m.youtube.com"
        private const val YOUTUBE_DESKTOP_URL = "https://www.youtube.com"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupWakeLock()
        setupWebView()
        setupNetworkCallback()
        setupBackPressedHandler()
        setupFab()
        checkAndShowTelegramDialog()
        requestBatteryOptimization()
        
        startMusicService()
    }
    
    private fun setupWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "YouTubeLite::Playback"
        )
    }
    
    private fun startMusicService() {
        val intent = Intent(this, MusicPlayerService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start music service", e)
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
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
            userAgentString = if (PreferenceManager.isDesktopMode) {
                DESKTOP_USER_AGENT
            } else {
                MOBILE_USER_AGENT
            }
            blockNetworkLoads = false
            blockNetworkImage = false
        }
        
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
            flush()
        }
        
        webView.addJavascriptInterface(
            WebAppInterface(this),
            WebAppInterface.INTERFACE_NAME
        )
        
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (PreferenceManager.isAdBlockEnabled && AdBlocker.isAd(request?.url.toString())) {
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
                
                injectAllScripts()
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
            private var customView: View? = null
            
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress == 100) {
                    binding.progressBar.visibility = View.GONE
                } else {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
            
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                
                customView = view
                customViewCallback = callback
                
                customViewContainer = FrameLayout(this@MainActivity).apply {
                    setBackgroundColor(0xFF000000.toInt())
                    addView(view, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    ))
                }
                
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
                
                setContentView(customViewContainer)
            }
            
            override fun onHideCustomView() {
                if (customView == null) return
                
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                
                customViewContainer?.removeAllViews()
                customViewContainer = null
                
                customView = null
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
        
        val url = if (PreferenceManager.isDesktopMode) YOUTUBE_DESKTOP_URL else YOUTUBE_URL
        webView.loadUrl(url)
    }
    
    private fun injectAllScripts() {
        injectBackgroundPlayScript()
        
        if (PreferenceManager.isAdBlockEnabled) {
            injectAdBlockScript()
        }
        
        WebAppInterface.injectJavaScript(binding.mainWebview)
        
        // Keep video playing
        keepVideoAlive()
    }
    
    @SuppressLint("JavascriptInterface")
    private fun injectBackgroundPlayScript() {
        val js = """
            (function() {
                'use strict';
                
                if (window.__bgPlayInjected) return;
                window.__bgPlayInjected = true;
                
                console.log('[YouTubeLite] Injecting background play...');
                
                // 1. Override visibility properties
                var overrideProps = function(obj, propName, value) {
                    try {
                        Object.defineProperty(obj, propName, {
                            get: function() { return value; },
                            set: function() {},
                            configurable: true,
                            enumerable: true
                        });
                    } catch(e) {}
                };
                
                overrideProps(document, 'hidden', false);
                overrideProps(document, 'visibilityState', 'visible');
                overrideProps(document, 'webkitHidden', false);
                overrideProps(document, 'webkitVisibilityState', 'visible');
                overrideProps(window, 'hidden', false);
                
                // 2. Block visibility events
                ['visibilitychange', 'webkitvisibilitychange', 'mozvisibilitychange', 
                 'msvisibilitychange', 'pagehide', 'pageshow', 'freeze', 'resume'].forEach(function(evt) {
                    document.addEventListener(evt, function(e) {
                        e.stopImmediatePropagation();
                        e.preventDefault();
                    }, true);
                    window.addEventListener(evt, function(e) {
                        e.stopImmediatePropagation();
                        e.preventDefault();
                    }, true);
                });
                
                // 3. Override EventTarget.addEventListener
                var origAdd = EventTarget.prototype.addEventListener;
                EventTarget.prototype.addEventListener = function(type, fn, opts) {
                    if (['visibilitychange', 'webkitvisibilitychange', 'blur', 'focusout'].indexOf(type) !== -1) {
                        return;
                    }
                    return origAdd.call(this, type, fn, opts);
                };
                
                // 4. Prevent video pause
                var origPause = HTMLVideoElement.prototype.pause;
                HTMLVideoElement.prototype.pause = function() {
                    if (this.__forcePause || this.ended || this.__userPaused) {
                        return origPause.apply(this, arguments);
                    }
                    console.log('[YouTubeLite] Blocked auto-pause');
                };
                
                // 5. Track user pause
                document.addEventListener('click', function(e) {
                    var btn = e.target.closest('.ytp-play-button') || 
                              e.target.closest('[aria-label*="Play"]') ||
                              e.target.closest('[aria-label*="Pause"]');
                    if (btn && document.querySelector('video')) {
                        document.querySelector('video').__userPaused = !document.querySelector('video').paused;
                    }
                }, true);
                
                document.addEventListener('keydown', function(e) {
                    if (e.code === 'Space' && document.querySelector('video')) {
                        document.querySelector('video').__userPaused = !document.querySelector('video').paused;
                    }
                }, true);
                
                console.log('[YouTubeLite] Background play ready!');
            })();
        """.trimIndent()
        
        binding.mainWebview.evaluateJavascript(js, null)
    }
    
    private fun injectAdBlockScript() {
        binding.mainWebview.evaluateJavascript(AdBlocker.getAdBlockCss(), null)
    }
    
    private fun keepVideoAlive() {
        binding.mainWebview.evaluateJavascript(
            """
            (function() {
                var video = document.querySelector('video');
                if (video && !video.paused && !video.ended) {
                    video.play().catch(function(){});
                }
            })();
            """,
            null
        )
    }
    
    private fun setupNetworkCallback() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
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
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
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
    }
    
    private fun hideOfflineView() {
        binding.offlineLayout.visibility = View.GONE
        binding.mainWebview.visibility = View.VISIBLE
    }
    
    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (customViewContainer != null) {
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
        binding.fabPip.setOnClickListener {
            enterPictureInPicture()
        }
    }
    
    private fun enterPictureInPicture(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                try {
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                setAutoEnterEnabled(true)
                            }
                        }
                        .build()
                    
                    return enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    Log.e(TAG, "PiP error", e)
                }
            }
        }
        return false
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPicture()
        }
        
        // Acquire wake lock for background playback
        wakeLock?.acquire(30 * 60 * 1000L)
        
        // Keep video playing
        keepVideoAlive()
    }
    
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        
        binding.fabPip.isVisible = !isInPictureInPictureMode
        
        if (isInPictureInPictureMode) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            keepVideoAlive()
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
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
            openTelegramChannel()
            dialog.dismiss()
        }
        
        dialogBinding.btnLater.setOnClickListener {
            PreferenceManager.isTgJoined = true
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun openTelegramChannel() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=JubairSensei")))
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/JubairSensei")))
        }
    }
    
    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Battery optimization request failed", e)
                }
            }
        }
    }
    
    fun hardReload() {
        CookieManager.getInstance().flush()
        binding.mainWebview.apply {
            clearCache(true)
            clearHistory()
        }
        
        val url = if (PreferenceManager.isDesktopMode) YOUTUBE_DESKTOP_URL else YOUTUBE_URL
        binding.mainWebview.loadUrl(url)
        
        Toast.makeText(this, "Reloaded", Toast.LENGTH_SHORT).show()
    }
    
    fun clearAllCache() {
        binding.mainWebview.apply {
            clearCache(true)
            clearHistory()
        }
        
        CookieManager.getInstance().removeAllCookies(null)
        
        try {
            cacheDir.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
    }
    
    fun toggleDesktopMode(enabled: Boolean) {
        PreferenceManager.isDesktopMode = enabled
        binding.mainWebview.settings.userAgentString = if (enabled) DESKTOP_USER_AGENT else MOBILE_USER_AGENT
        hardReload()
    }
    
    override fun onResume() {
        super.onResume()
        binding.mainWebview.onResume()
        injectAllScripts()
        
        // Release wake lock when coming back
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
    }
    
    override fun onPause() {
        // DO NOT call super.onPause() or webView.onPause() for background play!
        // Acquire wake lock
        wakeLock?.acquire(30 * 60 * 1000L)
        
        // Keep video alive
        keepVideoAlive()
    }
    
    override fun onStop() {
        // Keep video playing in background
        keepVideoAlive()
    }
    
    override fun onDestroy() {
        networkCallback?.let {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(it)
        }
        
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        
        try {
            stopService(Intent(this, MusicPlayerService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        super.onDestroy()
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { binding.mainWebview.loadUrl(it.toString()) }
    }
}

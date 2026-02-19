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
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sensei.youtube.R
import com.sensei.youtube.databinding.ActivityMainBinding
import com.sensei.youtube.databinding.DialogTelegramBinding
import com.sensei.youtube.services.NotificationService
import com.sensei.youtube.utils.AdBlocker
import com.sensei.youtube.utils.PreferenceManager
import com.sensei.youtube.utils.WebAppInterface

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var telegramDialogShown = false
    private var isInBackground = false
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    private val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    
    private val pipPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }
    
    companion object {
        private const val TAG = "MainActivity"
        private const val YOUTUBE_URL = "https://m.youtube.com"
        private const val YOUTUBE_DESKTOP_URL = "https://www.youtube.com"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        acquireWakeLock()
        setupWebView()
        setupNetworkCallback()
        setupBackPressedHandler()
        setupFab()
        checkAndShowTelegramDialog()
        requestBatteryOptimization()
    }
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "YouTubeLite::VideoPlayback"
        )
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
                
                injectBackgroundPlayScript()
                
                if (PreferenceManager.isAdBlockEnabled) {
                    injectAdBlockScript()
                }
                
                WebAppInterface.injectJavaScript(binding.mainWebview)
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
                if (newProgress == 100) {
                    binding.progressBar.visibility = View.GONE
                } else {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
        
        val url = if (PreferenceManager.isDesktopMode) YOUTUBE_DESKTOP_URL else YOUTUBE_URL
        webView.loadUrl(url)
    }
    
    private fun injectAdBlockScript() {
        binding.mainWebview.evaluateJavascript(AdBlocker.getAdBlockCss(), null)
    }
    
    @SuppressLint("JavascriptInterface")
    private fun injectBackgroundPlayScript() {
        val js = """
            (function() {
                'use strict';
                
                if (window.__backgroundPlayInjected) {
                    console.log('Background play already injected');
                    return;
                }
                window.__backgroundPlayInjected = true;
                
                console.log('Injecting background play script...');
                
                // Override document.hidden property
                try {
                    Object.defineProperty(document, 'hidden', {
                        get: function() { return false; },
                        configurable: true,
                        enumerable: true
                    });
                } catch(e) { console.log('hidden override failed', e); }
                
                // Override document.visibilityState property  
                try {
                    Object.defineProperty(document, 'visibilityState', {
                        get: function() { return 'visible'; },
                        configurable: true,
                        enumerable: true
                    });
                } catch(e) { console.log('visibilityState override failed', e); }
                
                // Override document.webkitHidden
                try {
                    Object.defineProperty(document, 'webkitHidden', {
                        get: function() { return false; },
                        configurable: true,
                        enumerable: true
                    });
                } catch(e) { console.log('webkitHidden override failed', e); }
                
                // Override document.webkitVisibilityState
                try {
                    Object.defineProperty(document, 'webkitVisibilityState', {
                        get: function() { return 'visible'; },
                        configurable: true,
                        enumerable: true
                    });
                } catch(e) { console.log('webkitVisibilityState override failed', e); }
                
                // Stop visibility change events from propagating
                var stopVisibilityEvents = function(e) {
                    e.stopImmediatePropagation();
                    e.preventDefault();
                    return false;
                };
                
                document.addEventListener('visibilitychange', stopVisibilityEvents, true);
                document.addEventListener('webkitvisibilitychange', stopVisibilityEvents, true);
                document.addEventListener('mozvisibilitychange', stopVisibilityEvents, true);
                document.addEventListener('msvisibilitychange', stopVisibilityEvents, true);
                
                // Also stop on window
                window.addEventListener('visibilitychange', stopVisibilityEvents, true);
                window.addEventListener('webkitvisibilitychange', stopVisibilityEvents, true);
                window.addEventListener('blur', stopVisibilityEvents, true);
                window.addEventListener('focusout', stopVisibilityEvents, true);
                
                // Override addEventListener to filter visibility events
                var originalAddEventListener = EventTarget.prototype.addEventListener;
                EventTarget.prototype.addEventListener = function(type, listener, options) {
                    if (type === 'visibilitychange' || type === 'webkitvisibilitychange' || 
                        type === 'mozvisibilitychange' || type === 'msvisibilitychange') {
                        console.log('Blocked visibility listener:', type);
                        return;
                    }
                    return originalAddEventListener.call(this, type, listener, options);
                };
                
                // Override pagehide and pageshow events
                window.addEventListener('pagehide', function(e) {
                    e.stopImmediatePropagation();
                    e.preventDefault();
                }, true);
                
                window.addEventListener('freeze', function(e) {
                    e.stopImmediatePropagation();
                    e.preventDefault();
                }, true);
                
                // Keep video playing function
                window.__keepVideoPlaying = function() {
                    var videos = document.querySelectorAll('video');
                    videos.forEach(function(video) {
                        if (video.paused && video.currentTime > 0 && !video.ended) {
                            var playPromise = video.play();
                            if (playPromise !== undefined) {
                                playPromise.catch(function(error) {
                                    console.log('Video play failed:', error);
                                });
                            }
                        }
                        
                        // Remove pause event listeners that might be added by YouTube
                        video.onpause = null;
                        video.removeAttribute('onpause');
                    });
                };
                
                // Run keep alive every 500ms
                setInterval(window.__keepVideoPlaying, 500);
                
                // Also run when page visibility would normally change
                setInterval(function() {
                    window.__keepVideoPlaying();
                    
                    // Dispatch fake visibility events
                    var visibleEvent = new Event('visibilitychange', { bubbles: false });
                    Object.defineProperty(visibleEvent, 'target', { value: document, enumerable: true });
                }, 1000);
                
                // Override HTMLVideoElement.pause to prevent unwanted pauses
                var originalPause = HTMLVideoElement.prototype.pause;
                HTMLVideoElement.prototype.pause = function() {
                    // Only allow pause if video is at end or user explicitly requested
                    if (this.ended || this.__userPaused) {
                        return originalPause.call(this);
                    }
                    console.log('Blocked automatic pause');
                    return;
                };
                
                // Track user-initiated pauses
                document.addEventListener('keydown', function(e) {
                    if (e.code === 'Space' || e.code === 'MediaPlayPause') {
                        var videos = document.querySelectorAll('video');
                        videos.forEach(function(v) {
                            v.__userPaused = !v.paused;
                        });
                    }
                });
                
                document.addEventListener('click', function(e) {
                    var target = e.target;
                    if (target.closest('.ytp-play-button') || target.closest('.ytp-button[aria-label*="Play"]') || 
                        target.closest('.ytp-button[aria-label*="Pause"]')) {
                        var videos = document.querySelectorAll('video');
                        videos.forEach(function(v) {
                            v.__userPaused = !v.paused;
                        });
                    }
                });
                
                console.log('Background play script injected successfully!');
            })();
        """.trimIndent()
        
        binding.mainWebview.evaluateJavascript(js) { result ->
            Log.d(TAG, "Background play script result: $result")
        }
    }
    
    private fun setupNetworkCallback() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    hideOfflineView()
                }
            }
            
            override fun onLost(network: Network) {
                runOnUiThread {
                    showOfflineView()
                }
            }
        }
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    private fun showOfflineView() {
        binding.offlineLayout.visibility = View.VISIBLE
        binding.mainWebview.visibility = View.GONE
        
        binding.btnRetry.setOnClickListener {
            if (isNetworkAvailable()) {
                hideOfflineView()
                binding.mainWebview.reload()
            } else {
                Toast.makeText(this, "Still offline", Toast.LENGTH_SHORT).show()
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
                if (binding.mainWebview.canGoBack()) {
                    binding.mainWebview.goBack()
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        moveTaskToBack(true)
                    } else {
                        finish()
                    }
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
                        .setAspectRatio(android.util.Rational(16, 9))
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
        if (PreferenceManager.isBackgroundPlayEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPicture()
            startBackgroundPlayback()
        }
    }
    
    private fun startBackgroundPlayback() {
        isInBackground = true
        
        // Acquire wake lock
        wakeLock?.acquire(30 * 60 * 1000L) // 30 minutes
        
        // Start foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, NotificationService::class.java))
        } else {
            startService(Intent(this, NotificationService::class.java))
        }
        
        // Keep WebView active
        binding.mainWebview.settings.mediaPlaybackRequiresUserGesture = false
        
        // Re-inject background play script
        injectBackgroundPlayScript()
        
        // Force video to keep playing
        binding.mainWebview.evaluateJavascript(
            """
            (function() {
                var videos = document.querySelectorAll('video');
                videos.forEach(function(v) {
                    v.play().catch(function(){});
                });
            })();
            """,
            null
        )
    }
    
    private fun stopBackgroundPlayback() {
        isInBackground = false
        
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        
        stopService(Intent(this, NotificationService::class.java))
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
            
            // Ensure video keeps playing
            binding.mainWebview.evaluateJavascript(
                """
                (function() {
                    var video = document.querySelector('video');
                    if (video) video.play();
                })();
                """,
                null
            )
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
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=JubairSensei"))
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/JubairSensei"))
            startActivity(intent)
        }
    }
    
    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                pipPermissionLauncher.launch(intent)
            }
        }
    }
    
    fun hardReload() {
        CookieManager.getInstance().flush()
        binding.mainWebview.apply {
            clearCache(true)
            clearHistory()
            clearFormData()
        }
        
        val url = if (PreferenceManager.isDesktopMode) YOUTUBE_DESKTOP_URL else YOUTUBE_URL
        binding.mainWebview.loadUrl(url)
        
        Toast.makeText(this, "Cache cleared and page reloaded", Toast.LENGTH_SHORT).show()
    }
    
    fun clearAllCache() {
        binding.mainWebview.apply {
            clearCache(true)
            clearHistory()
            clearFormData()
        }
        
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        
        deleteDatabase("webview.db")
        deleteDatabase("webviewCache.db")
        
        cacheDir.deleteRecursively()
        
        Toast.makeText(this, "All cache cleared", Toast.LENGTH_SHORT).show()
    }
    
    fun toggleDesktopMode(enabled: Boolean) {
        PreferenceManager.isDesktopMode = enabled
        binding.mainWebview.settings.userAgentString = if (enabled) {
            DESKTOP_USER_AGENT
        } else {
            MOBILE_USER_AGENT
        }
        hardReload()
    }
    
    override fun onResume() {
        super.onResume()
        binding.mainWebview.onResume()
        
        // Re-inject background play script
        injectBackgroundPlayScript()
        
        // Resume video playback
        binding.mainWebview.evaluateJavascript(
            """
            (function() {
                var videos = document.querySelectorAll('video');
                videos.forEach(function(v) {
                    if (!v.ended && v.currentTime > 0) {
                        v.play().catch(function(){});
                    }
                });
            })();
            """,
            null
        )
    }
    
    override fun onPause() {
        super.onPause()
        
        if (PreferenceManager.isBackgroundPlayEnabled) {
            // DON'T call webView.onPause() - this pauses the video
            // Instead, start background playback
            startBackgroundPlayback()
            
            // Force keep playing
            binding.mainWebview.evaluateJavascript(
                """
                (function() {
                    window.__keepVideoPlaying && window.__keepVideoPlaying();
                    var videos = document.querySelectorAll('video');
                    videos.forEach(function(v) {
                        if (!v.ended) {
                            v.play().catch(function(){});
                        }
                    });
                })();
                """,
                null
            )
        } else {
            binding.mainWebview.onPause()
        }
    }
    
    override fun onDestroy() {
        networkCallback?.let {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(it)
        }
        
        stopBackgroundPlayback()
        
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        
        super.onDestroy()
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { uri ->
            binding.mainWebview.loadUrl(uri.toString())
        }
    }
}

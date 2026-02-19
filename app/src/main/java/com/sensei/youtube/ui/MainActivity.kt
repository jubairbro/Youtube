package com.sensei.youtube.ui

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sensei.youtube.R
import com.sensei.youtube.databinding.ActivityMainBinding
import com.sensei.youtube.databinding.DialogTelegramBinding
import com.sensei.youtube.service.AudioService
import com.sensei.youtube.util.AdBlock
import com.sensei.youtube.util.JSBridge
import com.sensei.youtube.util.Prefs

class MainActivity : AppCompatActivity() {
    
    private lateinit var bind: ActivityMainBinding
    private var netCallback: ConnectivityManager.NetworkCallback? = null
    private var customView: FrameLayout? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null
    private var isFs = false
    private var isAudioMode = false
    
    companion object {
        private const val TAG = "Main"
        private const val URL = "https://m.youtube.com"
        private const val URL_D = "https://www.youtube.com"
        
        private const val UA_M = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
        private const val UA_D = "Mozilla/5.0 (Windows NT 11.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }
    
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.root)
        
        setupWv()
        setupNet()
        setupBack()
        setupFabs()
        setupAudioPanel()
        showTg()
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWv() {
        val wv = bind.webview
        
        wv.settings.apply {
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
            builtInZoomControls = true
            displayZoomControls = false
            blockNetworkImage = false
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            userAgentString = if (Prefs.desktop) UA_D else UA_M
        }
        
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(wv, true)
            flush()
        }
        
        wv.addJavascriptInterface(JSBridge(this), JSBridge.NAME)
        
        wv.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(v: WebView?, r: WebResourceRequest?): WebResourceResponse? {
                val url = r?.url.toString()
                
                if (url.contains(".jpg") || url.contains(".png") || 
                    url.contains(".webp") || url.contains(".gif")) {
                    return super.shouldInterceptRequest(v, r)
                }
                
                if (Prefs.adBlock && AdBlock.isAd(url)) {
                    return WebResourceResponse("text/plain", "utf-8", null)
                }
                return super.shouldInterceptRequest(v, r)
            }
            
            override fun onPageStarted(v: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(v, url, favicon)
                bind.progress.visibility = View.VISIBLE
            }
            
            override fun onPageFinished(v: WebView?, url: String?) {
                super.onPageFinished(v, url)
                bind.progress.visibility = View.GONE
                
                if (Prefs.adBlock) {
                    wv.evaluateJavascript(AdBlock.CSS, null)
                }
                injectBgPlay()
                JSBridge.inject(wv)
            }
            
            override fun onReceivedSslError(v: WebView?, h: SslErrorHandler?, e: SslError?) {
                h?.proceed()
            }
            
            override fun onReceivedError(v: WebView?, e: WebResourceRequest?, err: WebResourceError?) {
                super.onReceivedError(v, e, err)
                if (!isOnline()) showOffline()
            }
        }
        
        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(v: WebView?, p: Int) {
                bind.progress.visibility = if (p == 100) View.GONE else View.VISIBLE
            }
            
            override fun onShowCustomView(view: View?, cb: CustomViewCallback?) {
                if (customView != null) {
                    cb?.onCustomViewHidden()
                    return
                }
                
                isFs = true
                customCallback = cb
                customView = FrameLayout(this@MainActivity).apply {
                    setBackgroundColor(0xFF000000.toInt())
                    addView(view, FrameLayout.LayoutParams(-1, -1))
                }
                
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                setContentView(customView)
            }
            
            override fun onHideCustomView() {
                if (customView == null) return
                
                isFs = false
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                
                customView?.removeAllViews()
                customView = null
                customCallback?.onCustomViewHidden()
                customCallback = null
                
                setContentView(bind.root)
            }
            
            override fun onPermissionRequest(r: PermissionRequest?) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    r?.grant(r.resources)
                }
            }
        }
        
        load()
    }
    
    private fun injectBgPlay() {
        val js = """
            (function(){
                if(window._bgp)return;
                window._bgp=1;
                
                // Override visibility
                try{
                    Object.defineProperty(document,'hidden',{get:function(){return false},configurable:true});
                    Object.defineProperty(document,'visibilityState',{get:function(){return'visible'},configurable:true});
                    Object.defineProperty(document,'webkitHidden',{get:function(){return false},configurable:true});
                }catch(e){}
                
                // Block visibility events
                var block=function(e){e.stopImmediatePropagation();e.preventDefault();};
                ['visibilitychange','webkitvisibilitychange','pagehide','freeze','blur'].forEach(function(t){
                    document.addEventListener(t,block,true);
                    window.addEventListener(t,block,true);
                });
                
                // Prevent auto-pause
                var _pause=HTMLVideoElement.prototype.pause;
                HTMLVideoElement.prototype.pause=function(){
                    if(this._upause||this.ended)return _pause.apply(this,arguments);
                };
                
                // Track user pause
                document.addEventListener('click',function(e){
                    if(e.target.closest('.ytp-play-button')){
                        var v=document.querySelector('video');
                        if(v)v._upause=!v.paused;
                    }
                },true);
                
                // Keep alive
                setInterval(function(){
                    var v=document.querySelector('video');
                    if(v&&!v.paused===false&&!v.ended&&!v._upause)v.play().catch(function(){});
                },500);
            })();
        """.trimIndent()
        
        bind.webview.evaluateJavascript(js, null)
    }
    
    private fun setupNet() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val req = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        netCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(n: Network) { runOnUiThread { hideOffline() } }
            override fun onLost(n: Network) { runOnUiThread { showOffline() } }
        }
        cm.registerNetworkCallback(req, netCallback!!)
    }
    
    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val n = cm.activeNetwork ?: return false
        val c = cm.getNetworkCapabilities(n) ?: return false
        return c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    private fun showOffline() {
        bind.offline.visibility = View.VISIBLE
        bind.webview.visibility = View.GONE
        bind.retry.setOnClickListener {
            if (isOnline()) { hideOffline(); bind.webview.reload() }
        }
    }
    
    private fun hideOffline() {
        bind.offline.visibility = View.GONE
        bind.webview.visibility = View.VISIBLE
    }
    
    private fun setupBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFs && customCallback != null) {
                    bind.webview.webChromeClient?.onHideCustomView()
                    return
                }
                if (bind.webview.canGoBack()) bind.webview.goBack()
                else moveTaskToBack(true)
            }
        })
    }
    
    private fun setupFabs() {
        // PiP button
        bind.fab.setOnClickListener { enterPip() }
        
        // Audio mode button
        bind.fabAudio.setOnClickListener { toggleAudioMode() }
    }
    
    private fun setupAudioPanel() {
        bind.btnAudioPause.setOnClickListener {
            if (AudioService.isPlaying()) {
                AudioService.pause()
                bind.btnAudioPause.setImageResource(R.drawable.ic_play)
            } else {
                AudioService.play()
                bind.btnAudioPause.setImageResource(R.drawable.ic_pause)
            }
        }
        
        bind.btnAudioClose.setOnClickListener {
            hideAudioPanel()
            AudioService.stop(this)
        }
    }
    
    private fun toggleAudioMode() {
        isAudioMode = !isAudioMode
        
        if (isAudioMode) {
            // Extract and play audio
            Toast.makeText(this, "🎵 Audio Mode Activated", Toast.LENGTH_SHORT).show()
            
            // Get current video info and play
            bind.webview.evaluateJavascript("""
                (function(){
                    var v=document.querySelector('video');
                    if(v){
                        v.play();
                        window.Android && Android.onAudioMode(true);
                    }
                })();
            """.trimIndent(), null)
            
            showAudioPanel()
        } else {
            hideAudioPanel()
        }
    }
    
    private fun showAudioPanel() {
        bind.audioPanel.visibility = View.VISIBLE
        bind.audioTitle.text = AudioService.title
        bind.audioChannel.text = AudioService.author
        bind.btnAudioPause.setImageResource(
            if (AudioService.isPlaying()) R.drawable.ic_pause else R.drawable.ic_play
        )
        
        // Hide video if in audio mode
        bind.webview.evaluateJavascript("""
            (function(){
                var v=document.querySelector('video');
                if(v)v.style.opacity='0';
            })();
        """.trimIndent(), null)
    }
    
    private fun hideAudioPanel() {
        isAudioMode = false
        bind.audioPanel.visibility = View.GONE
        
        // Show video again
        bind.webview.evaluateJavascript("""
            (function(){
                var v=document.querySelector('video');
                if(v)v.style.opacity='1';
            })();
        """.trimIndent(), null)
    }
    
    fun updateAudioPanel(title: String, channel: String) {
        runOnUiThread {
            bind.audioTitle.text = title
            bind.audioChannel.text = channel
        }
    }
    
    private fun enterPip(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                return try {
                    val p = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                setAutoEnterEnabled(true)
                            }
                        }
                        .build()
                    enterPictureInPictureMode(p)
                } catch (e: Exception) { false }
            }
        }
        return false
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Prefs.bgPlay) {
            enterPip()
        }
    }
    
    override fun onPictureInPictureModeChanged(inPip: Boolean, c: Configuration) {
        super.onPictureInPictureModeChanged(inPip, c)
        bind.fab.isVisible = !inPip
        bind.fabAudio.isVisible = !inPip
        if (inPip) {
            bind.webview.evaluateJavascript("(function(){var v=document.querySelector('video');if(v)v.play()})();", null)
        }
    }
    
    private fun showTg() {
        if (!Prefs.tgShown) {
            val d = DialogTelegramBinding.inflate(layoutInflater)
            val diag = MaterialAlertDialogBuilder(this)
                .setView(d.root)
                .setCancelable(true)
                .create()
            diag.window?.setBackgroundDrawableResource(android.R.color.transparent)
            
            d.btnJoin.setOnClickListener {
                Prefs.tgShown = true
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=JubairSensei"))) }
                catch (e: Exception) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/JubairSensei"))) }
                diag.dismiss()
            }
            d.btnLater.setOnClickListener { Prefs.tgShown = true; diag.dismiss() }
            diag.show()
        }
    }
    
    private fun load() {
        bind.webview.loadUrl(if (Prefs.desktop) URL_D else URL)
    }
    
    fun reload() {
        CookieManager.getInstance().flush()
        bind.webview.clearCache(true)
        bind.webview.clearHistory()
        load()
        Toast.makeText(this, "Reloaded", Toast.LENGTH_SHORT).show()
    }
    
    fun clearCache() {
        bind.webview.clearCache(true)
        bind.webview.clearHistory()
        CookieManager.getInstance().removeAllCookies(null)
        cacheDir.deleteRecursively()
        Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
    }
    
    fun toggleDesktop(on: Boolean) {
        Prefs.desktop = on
        bind.webview.settings.userAgentString = if (on) UA_D else UA_M
        reload()
    }
    
    override fun onResume() {
        super.onResume()
        bind.webview.onResume()
        bind.webview.evaluateJavascript(AdBlock.CSS, null)
        injectBgPlay()
        JSBridge.inject(bind.webview)
    }
    
    override fun onPause() {
        super.onPause()
        if (Prefs.bgPlay) {
            bind.webview.evaluateJavascript("(function(){var v=document.querySelector('video');if(v&&!v.ended)v.play()})();", null)
            
            val intent = Intent(this, AudioService::class.java).apply {
                action = AudioService.ACTION_UPDATE
            }
            try { startService(intent) } catch (e: Exception) {}
        }
    }
    
    override fun onDestroy() {
        netCallback?.let {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(it)
        }
        AudioService.stop(this)
        super.onDestroy()
    }
    
    override fun onNewIntent(i: Intent?) {
        super.onNewIntent(i)
        i?.data?.let { bind.webview.loadUrl(it.toString()) }
    }
}

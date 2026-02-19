package com.sensei.youtube.utils

import android.content.Context
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object AdBlocker {
    
    private lateinit var context: Context
    private var isEnabled = true
    
    private val AD_HOSTS = setOf(
        "googleads.g.doubleclick.net",
        "pagead2.googlesyndication.com",
        "ads.google.com",
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "ad.doubleclick.net",
        "adservice.google.com",
        "pubads.g.doubleclick.net",
        "securepubads.g.doubleclick.net",
        "www.googletagservices.com",
        "partner.googleadservices.com",
        "tpc.googlesyndication.com",
        "googletagmanager.com",
        "www.googletagmanager.com",
        "ads.youtube.com"
    )
    
    private val IMAGE_HOSTS = setOf(
        "i.ytimg.com",
        "yt3.ggpht.com",
        "yt3.googleusercontent.com",
        "i9.ytimg.com",
        "i1.ytimg.com",
        "i2.ytimg.com",
        "i3.ytimg.com",
        "i4.ytimg.com"
    )
    
    fun init(context: Context) {
        this.context = context
    }
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    fun isAd(url: String?): Boolean {
        if (!isEnabled || url == null) return false
        
        if (url.contains(".jpg") || url.contains(".png") || url.contains(".webp") || url.contains(".gif")) {
            return false
        }
        
        val host = try {
            java.net.URL(url).host ?: return false
        } catch (e: Exception) {
            return false
        }
        
        if (IMAGE_HOSTS.any { host.contains(it, ignoreCase = true) }) {
            return false
        }
        
        return AD_HOSTS.any { adHost ->
            host.equals(adHost, ignoreCase = true) ||
            host.endsWith(".$adHost", ignoreCase = true)
        }
    }
    
    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            ByteArrayInputStream("".toByteArray())
        )
    }
    
    fun getAdBlockCss(): String {
        return """
            (function() {
                var style = document.createElement('style');
                style.type = 'text/css';
                style.innerHTML = `
                    .ytp-ad-module,
                    .ytp-ad-overlay-container,
                    .ytp-ad-text-overlay,
                    .video-ads,
                    .ytp-ad-progress-list,
                    ytd-promoted-video-renderer,
                    ytd-ad-slot-renderer,
                    ytd-in-feed-ad-layout-renderer,
                    ytd-banner-promo-renderer,
                    ytd-display-ad-renderer,
                    ytd-text-ad-renderer,
                    ytd-video-masthead-ad-v3-renderer,
                    ytd-mealbar-promo-renderer,
                    ytd-search-pyv-renderer,
                    ytd-promoted-sparkles-web-renderer,
                    .ytd-display-ad-renderer,
                    [class*="googlead"],
                    [class*="adsbygoogle"],
                    [id*="google_ads"],
                    #masthead-ad,
                    #player-ads,
                    ytd-engagement-panel-section-list-renderer[target-id="engagement-panel-ads"],
                    .ytp-ad-player-overlay,
                    .ytp-ad-skip-button-container,
                    tp-yt-iron-overlay-backdrop {
                        display: none !important; 
                        visibility: hidden !important;
                        opacity: 0 !important;
                    }
                    
                    .html5-video-player.ad-interrupting .video-ads,
                    .html5-video-player.ad-showing .video-ads {
                        display: none !important;
                    }
                `;
                document.head.appendChild(style);
            })();
        """.trimIndent()
    }
}

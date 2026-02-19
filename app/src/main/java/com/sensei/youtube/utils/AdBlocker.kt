package com.sensei.youtube.utils

import android.content.Context
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.IOException

object AdBlocker {
    
    private lateinit var context: Context
    private val blockedHosts = mutableSetOf<String>()
    private var isEnabled = true
    
    private val AD_HOSTS = listOf(
        "googleads.g.doubleclick.net",
        "pagead2.googlesyndication.com",
        "ads.google.com",
        "ads.youtube.com",
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "googleads.g.doubleclick.net",
        "ad.doubleclick.net",
        "static.doubleclick.net",
        "adservice.google.com",
        "adservice.google.ca",
        "adservice.google.co.uk",
        "pubads.g.doubleclick.net",
        "securepubads.g.doubleclick.net",
        "www.googletagservices.com",
        "partner.googleadservices.com",
        "tpc.googlesyndication.com",
        "s0.2mdn.net",
        "s1.2mdn.net",
        "s2.2mdn.net",
        "s3.2mdn.net",
        "fonts.googleapis.com",
        "yt3.ggpht.com",
        "i.ytimg.com"
    )
    
    fun init(context: Context) {
        this.context = context
        loadBlockedHosts()
    }
    
    private fun loadBlockedHosts() {
        blockedHosts.clear()
        blockedHosts.addAll(AD_HOSTS)
        try {
            context.assets.open("adblock_hosts.txt").bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    val host = line.trim()
                    if (host.isNotEmpty() && !host.startsWith("#")) {
                        blockedHosts.add(host)
                    }
                }
            }
        } catch (e: IOException) {
            // File doesn't exist, use default list
        }
    }
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    fun isAd(url: String?): Boolean {
        if (!isEnabled || url == null) return false
        
        val host = try {
            java.net.URL(url).host ?: return false
        } catch (e: Exception) {
            return false
        }
        
        return blockedHosts.any { blockedHost ->
            host.contains(blockedHost, ignoreCase = true) ||
            host.endsWith(".$blockedHost", ignoreCase = true)
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
                    [id*="ad-container"],
                    [class*="-ad-"],
                    [class*="ad-showing"],
                    .ytp-ad-player-overlay,
                    .ytp-ad-skip-button-container,
                    ytd-engagement-panel-section-list-renderer[target-id="engagement-panel-ads"],
                    #masthead-ad,
                    #player-ads,
                    #related ytd-ad-slot-renderer,
                    ytd-action-companion-ad-renderer,
                    ytd-companion-slot-renderer,
                    ytd-iframe-companion-renderer,
                    .ytp-ce-element.ytp-ce-element-show,
                    #merch-shelf,
                    ytd-merch-shelf-renderer,
                    yt-mealbar-promo-renderer,
                    tp-yt-iron-overlay-backward-compat,
                    .style-scope.ytd-rich-section-renderer,
                    ytd-statement-banner-renderer,
                    ytd-brand-video-singleton-renderer,
                    ytd-brand-video-shelf-renderer,
                    #top { display: none !important; visibility: hidden !important; }
                    
                    .html5-video-player.ad-interrupting .video-ads,
                    .html5-video-player.ad-showing .video-ads,
                    .ytp-ad-overlay-container,
                    .ytp-ad-text,
                    .ytp-ad-preview-text,
                    .ytp-ad-skip-button,
                    .ytp-ad-skip-button-modern { display: none !important; }
                `;
                document.head.appendChild(style);
                
                function removeAds() {
                    var adSelectors = [
                        '.ytp-ad-module',
                        '.ytp-ad-overlay-container', 
                        '.video-ads',
                        'ytd-ad-slot-renderer',
                        'ytd-display-ad-renderer',
                        'ytd-promoted-video-renderer',
                        'ytd-in-feed-ad-layout-renderer',
                        '#masthead-ad',
                        '#player-ads',
                        'ytd-engagement-panel-section-list-renderer[target-id="engagement-panel-ads"]'
                    ];
                    
                    adSelectors.forEach(function(selector) {
                        var elements = document.querySelectorAll(selector);
                        elements.forEach(function(el) {
                            el.remove();
                        });
                    });
                }
                
                removeAds();
                
                var observer = new MutationObserver(function(mutations) {
                    removeAds();
                });
                
                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });
                
                setInterval(removeAds, 1000);
            })();
        """.trimIndent()
    }
}

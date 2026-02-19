package com.sensei.youtube.util

object AdBlock {
    
    private val AD_DOMAINS = setOf(
        "googleads.g.doubleclick.net",
        "pagead2.googlesyndication.com",
        "ads.google.com",
        "ads.youtube.com",
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "ad.doubleclick.net",
        "adservice.google.com",
        "pubads.g.doubleclick.net",
        "securepubads.g.doubleclick.net",
        "partner.googleadservices.com",
        "tpc.googlesyndication.com",
        "googletagmanager.com",
        "adservice.google",
        "googlesyndication",
        "googleads",
        "doubleclick"
    )
    
    private val ALLOW_DOMAINS = setOf(
        "ytimg.com",
        "ggpht.com",
        "googleusercontent.com",
        "youtube.com",
        "googlevideo.com",
        "google.com",
        "gstatic.com"
    )
    
    fun isAd(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        
        // Always allow images
        if (url.contains(".jpg") || url.contains(".png") || 
            url.contains(".webp") || url.contains(".gif")) {
            return false
        }
        
        // Allow certain domains
        val allowHost = ALLOW_DOMAINS.any { url.contains(it, ignoreCase = true) }
        if (allowHost) return false
        
        // Check ad domains
        return AD_DOMAINS.any { ad -> 
            url.contains(ad, ignoreCase = true)
        }
    }
    
    val CSS: String = """
        (function(){
            if(window._adcss)return;
            window._adcss=1;
            var s=document.createElement('style');
            s.innerHTML=`
                .ytp-ad-module,.ytp-ad-overlay-container,.video-ads,
                .ytp-ad-progress-list,ytd-ad-slot-renderer,ytd-display-ad-renderer,
                ytd-promoted-video-renderer,ytd-in-feed-ad-layout-renderer,
                #masthead-ad,#player-ads,.ytp-ad-player-overlay,
                tp-yt-iron-overlay-backdrop,ytd-mealbar-promo-renderer,
                ytd-engagement-panel-section-list-renderer[target-id="engagement-panel-ads"],
                .ytp-ad-text,.ytp-ad-skip-button,ytd-banner-promo-renderer,
                ytd-statement-banner-renderer,ytd-brand-video-shelf-renderer,
                [class*="-ad-"],[class*="ad-container"],[id*="ad-container"],
                .ytd-display-ad-renderer,ytd-action-companion-ad-renderer{
                    display:none!important;visibility:hidden!important;opacity:0!important;
                }
            `;
            document.head.appendChild(s);
        })();
    """.trimIndent()
}

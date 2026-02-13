package com.jubair.youtube.utils;

public class ScriptInjector {
    public static String getRemoveAdsScript() {
        return "javascript:(function() {" +
                "var style = document.createElement('style');" +
                "style.innerHTML = '" +
                // --- CSS From GoodTube Source ---
                ".ytd-search ytd-shelf-renderer, ytd-reel-shelf-renderer, ytd-merch-shelf-renderer, ytd-action-companion-ad-renderer, ytd-display-ad-renderer { display: none !important; }" +
                "ytd-video-masthead-ad-advertiser-info-renderer, ytd-video-masthead-ad-primary-video-renderer, ytd-in-feed-ad-layout-renderer, ytd-ad-slot-renderer, ytd-statement-banner-renderer { display: none !important; }" +
                ".video-ads, .ytp-ad-overlay-container, .ytp-ad-message-container, .ytp-ad-skip-button-slot { display: none !important; }" +
                "ytm-promoted-sparkles-web-renderer, ytm-brand-banner-renderer, ytm-item-section-renderer[data-is-ad] { display: none !important; }" +
                ".mobile-topbar-header, .pivot-bar-renderer, .ytm-app-upsell { display: none !important; }" +
                
                // Shorts Hiding
                "ytm-pivot-bar-item-renderer:has(> .pivot-shorts), ytd-rich-section-renderer, ytm-reel-shelf-renderer { display: none !important; }" +
                "';" +
                "document.head.appendChild(style);" +

                // --- Auto Skip Logic ---
                "setInterval(function(){" +
                "   try {" +
                "       var skipBtn = document.querySelector('.ytp-ad-skip-button');" +
                "       if(skipBtn) skipBtn.click();" +
                "       var overlayClose = document.querySelector('.ytp-ad-overlay-close-button');" +
                "       if(overlayClose) overlayClose.click();" +
                "       var video = document.querySelector('video');" +
                "       if(video && video.paused && video.currentTime > 0) video.play();" +
                "   } catch(e) {}" +
                "}, 500);" +
                "})()";
    }
}

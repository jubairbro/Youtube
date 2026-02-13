package com.jubair.youtube.utils;

public class ScriptInjector {
    public static String getRemoveAdsScript() {
        return "javascript:(function() {" +
                // 1. CSS Injection (Hide Elements)
                "var style = document.createElement('style');" +
                "style.innerHTML = '" +
                ".mobile-topbar-header, .pivot-bar-renderer, .ytm-app-upsell { display: none !important; }" +
                ".ad-container, .ad-interrupting, .video-ads, .ytp-ad-overlay-container { display: none !important; }" +
                ".ytm-promoted-sparkles-web-renderer, ytm-statement-banner-renderer { display: none !important; }" +
                "';" +
                "document.head.appendChild(style);" +

                // 2. JS Bypass (Force Skip & Popup Remove)
                "setInterval(function(){" +
                "   try {" +
                "       var skipBtn = document.querySelector('.ytp-ad-skip-button');" +
                "       if(skipBtn) skipBtn.click();" +
                "       var overlayClose = document.querySelector('.ytp-ad-overlay-close-button');" +
                "       if(overlayClose) overlayClose.click();" +
                "       " +
                "       // Remove 'Adblock Detected' Popup" +
                "       var popup = document.querySelector('yt-playability-error-supported-renderers');" +
                "       if(popup) { popup.remove(); }" +
                "       var backdrop = document.querySelector('tp-yt-iron-overlay-backdrop');" +
                "       if(backdrop) { backdrop.remove(); }" +
                "       " +
                "       // Force Play if paused by adblock detection" +
                "       var video = document.querySelector('video');" +
                "       if(video && video.paused && video.currentTime > 0 && !video.ended) { video.play(); }" +
                "   } catch(e) {}" +
                "}, 500);" +
                
                // 3. Prevent Ad Scripts (Experimental)
                "if(window.yt && window.yt.config_) {" +
                "   window.yt.config_.openPopupConfig = { supportedPopups: { adBlockMessageViewModel: false } };" +
                "}" +
                "})()";
    }
}

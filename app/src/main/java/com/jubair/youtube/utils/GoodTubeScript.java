package com.jubair.youtube.utils;

public class GoodTubeScript {
    public static String getInjectScript() {
        return "javascript:(function() {" +
                // 1. CSS Cleanups
                "var style = document.createElement('style');" +
                "style.innerHTML = '" +
                ".ad-container, .ad-interrupting, .video-ads, .ytp-ad-overlay-container { display: none !important; }" +
                ".ytm-promoted-sparkles-web-renderer, ytm-statement-banner-renderer { display: none !important; }" +
                ".ytm-app-upsell, [aria-label=\"Open App\"] { display: none !important; }" +
                "ytm-companion-slot, ytm-promoted-video-renderer { display: none !important; }" +
                "ytm-pivot-bar-item-renderer:has(> .pivot-shorts), ytd-reel-shelf-renderer { display: none !important; }" +
                "body.video-playing ytm-mobile-topbar-renderer { display: none !important; }" +
                "body.video-playing .mobile-topbar-header { display: none !important; }" +
                "body.video-playing ytm-pivot-bar-renderer { display: none !important; }" +
                "body.video-playing #header-bar { display: none !important; }" +
                "body.video-playing ytm-search { display: none !important; }" +
                "body.video-playing #player-container-id { position: fixed !important; top: 0 !important; width: 100% !important; z-index: 9999 !important; background: #000 !important; }" +
                "';" +
                "document.head.appendChild(style);" +

                // 2. Logic Loop (Unmute + Auto Skip)
                "setInterval(function(){" +
                "   try {" +
                "       if (window.location.href.indexOf('/watch') > -1) {" +
                "           document.body.classList.add('video-playing');" +
                "       } else {" +
                "           document.body.classList.remove('video-playing');" +
                "       }" +

                "       // Auto Skip Ads" +
                "       var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-skip-ad-button');" +
                "       if(skipBtn) skipBtn.click();" +
                "       var overlayClose = document.querySelector('.ytp-ad-overlay-close-button');" +
                "       if(overlayClose) overlayClose.click();" +
                
                "       // Tap to Unmute Fix" +
                "       var unmute = document.querySelector('.ytp-unmute-inner');" +
                "       if(unmute && unmute.offsetParent !== null) { unmute.click(); }" +
                
                "       // Force Background Play" +
                "       var video = document.querySelector('video');" +
                "       if(video) {" +
                "           video.setAttribute('playsinline', 'true');" +
                "           if(!video.paused) { Android.onVideoPlay(); }" +
                "           else { Android.onVideoPause(); }" +
                "       }" +
                "   } catch(e) {}" +
                "}, 1000);" +
                
                // 3. Media Handler
                "window.handleMediaKey = function(key) {" +
                "   var video = document.querySelector('video');" +
                "   if(video) {" +
                "       if(key === 'play') video.play();" +
                "       if(key === 'pause') video.pause();" +
                "       if(key === 'next') document.querySelector('.ytp-next-button').click();" +
                "       if(key === 'prev') document.querySelector('.ytp-prev-button').click();" +
                "   }" +
                "};" +
                "})()";
    }
}

package com.jubair.youtube.utils;

public class GhostScript {
    public static String getHackingScript() {
        return "javascript:(function() {" +
                // 1. Page Visibility API Hack (Background Play Fix)
                "Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });" +
                "Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });" +
                "document.dispatchEvent(new Event('visibilitychange'));" +
                
                // 2. Audio Focus Hack (যাতে স্ক্রিন অফ করলে পজ না হয়)
                "window.addEventListener('blur', function(e) { e.stopImmediatePropagation(); }, true);" +
                "window.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);" +
                "window.addEventListener('webkitvisibilitychange', function(e) { e.stopImmediatePropagation(); }, true);" +
                
                // 3. UI Cleanup (Header/Footer Removal)
                "var style = document.createElement('style');" +
                "style.innerHTML = '" +
                "   .mobile-topbar-header, ytm-mobile-topbar-renderer { display: none !important; }" +
                "   .pivot-bar-renderer, ytm-pivot-bar-renderer { display: none !important; }" +
                "   .ytm-app-upsell, [aria-label=\"Open App\"] { display: none !important; }" +
                "   .ad-container, .video-ads, .ytp-ad-overlay-container { display: none !important; }" +
                "';" +
                "document.head.appendChild(style);" +

                // 4. Force Video Play (যদি তবুও পজ হয়ে যায়)
                "setInterval(function(){" +
                "   var video = document.querySelector('video');" +
                "   if(video && video.paused && video.currentTime > 0 && !video.ended) {" +
                "       video.play();" +
                "   }" +
                "   // Auto Skip Ads" +
                "   var skip = document.querySelector('.ytp-ad-skip-button');" +
                "   if(skip) skip.click();" +
                "}, 1000);" +
                
                "})()";
    }
}

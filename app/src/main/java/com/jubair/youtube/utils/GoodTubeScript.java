package com.jubair.youtube.utils;

public class GoodTubeScript {
    public static String getInjectScript() {
        return "javascript:(function() {" +
                // --- ১. ব্যাকগ্রাউন্ড প্লে হ্যাক (Visibility API Hack) ---
                "Object.defineProperty(document, 'hidden', { value: false, writable: false });" +
                "Object.defineProperty(document, 'visibilityState', { value: 'visible', writable: false });" +
                "window.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);" +

                // --- ২. CSS ক্লিনআপ (আগের মতোই) ---
                "var style = document.createElement('style');" +
                "style.innerHTML = '" +
                ".ad-container, .ad-interrupting, .video-ads, .ytp-ad-overlay-container { display: none !important; }" +
                ".ytm-promoted-sparkles-web-renderer, ytm-statement-banner-renderer { display: none !important; }" +
                ".ytm-app-upsell, [aria-label=\"Open App\"] { display: none !important; }" +
                "body.video-playing ytm-mobile-topbar-renderer, body.video-playing .mobile-topbar-header { display: none !important; }" +
                "body.video-playing ytm-pivot-bar-renderer, body.video-playing #header-bar { display: none !important; }" +
                "body.video-playing #player-container-id { position: fixed !important; top: 0 !important; width: 100% !important; z-index: 9999 !important; background: #000 !important; }" +
                "';" +
                "document.head.appendChild(style);" +

                // --- ৩. মিডিয়া ইভেন্ট লিসেনার (অডিও প্লেয়ার ফিক্স) ---
                "document.addEventListener('play', function(e){" +
                "   if(e.target.tagName === 'VIDEO'){ " +
                "       Android.onVideoPlay();" +
                "       e.target.setAttribute('playsinline', 'true');" +
                "   }" +
                "}, true);" +
                
                "document.addEventListener('pause', function(e){" +
                "   if(e.target.tagName === 'VIDEO'){ " +
                "       Android.onVideoPause();" +
                "   }" +
                "}, true);" +

                // --- ৪. অটো স্কিপ এবং আনমিউট লজিক ---
                "setInterval(function(){" +
                "   try {" +
                "       if (window.location.href.indexOf('/watch') > -1) {" +
                "           document.body.classList.add('video-playing');" +
                "       } else {" +
                "           document.body.classList.remove('video-playing');" +
                "       }" +
                "       var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-skip-ad-button');" +
                "       if(skipBtn) skipBtn.click();" +
                "       var unmute = document.querySelector('.ytp-unmute-inner');" +
                "       if(unmute) unmute.click();" +
                "   } catch(e) {}" +
                "}, 1000);" +
                "})()";
    }
}

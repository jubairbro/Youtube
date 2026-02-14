package com.jubair.youtube.utils;

public class ScriptInjector {
    public static String getInjectScript() {
        return "javascript:(function() {" +
                // 1. CSS Setup (Dynamic Class Based Hiding)
                "var style = document.createElement('style');" +
                "style.innerHTML = '" +
                // যখন 'video-mode' ক্লাস থাকবে, তখনই কেবল এগুলো হাইড হবে
                "body.video-mode ytm-mobile-topbar-renderer { display: none !important; }" +
                "body.video-mode .mobile-topbar-header { display: none !important; }" +
                "body.video-mode ytm-pivot-bar-renderer { display: none !important; }" +
                "body.video-mode .pivot-bar-renderer { display: none !important; }" +
                
                // অ্যাড সবসময় ব্লক থাকবে (সব পেজে)
                ".ad-container, .ad-interrupting, .video-ads, .ytp-ad-overlay-container { display: none !important; }" +
                ".ytm-promoted-sparkles-web-renderer, ytm-statement-banner-renderer { display: none !important; }" +
                ".ytm-app-upsell, [aria-label=\"Open App\"] { display: none !important; }" +
                "';" +
                "document.head.appendChild(style);" +

                // 2. Logic Loop (URL Check & Auto Skip)
                "setInterval(function(){" +
                "   try {" +
                "       // URL চেক: যদি ভিডিও চলে (/watch), তবে 'video-mode' অন করো" +
                "       if (window.location.href.indexOf('/watch') > -1) {" +
                "           document.body.classList.add('video-mode');" +
                "       } else {" +
                "           document.body.classList.remove('video-mode');" +
                "       }" +
                
                "       // অটো অ্যাড স্কিপ এবং ব্যাকগ্রাউন্ড প্লে লজিক" +
                "       var skipBtn = document.querySelector('.ytp-ad-skip-button');" +
                "       if(skipBtn) skipBtn.click();" +
                "       var overlayClose = document.querySelector('.ytp-ad-overlay-close-button');" +
                "       if(overlayClose) overlayClose.click();" +
                "       " +
                "       var video = document.querySelector('video');" +
                "       if(video) {" +
                "           video.setAttribute('playsinline', 'true');" +
                "           // ব্যাকগ্রাউন্ডে পজ হওয়া আটকানো" +
                "           if(video.paused && !document.hidden && video.currentTime > 0) video.play();" +
                "       }" +
                "   } catch(e) {}" +
                "}, 500);" +
                "})()";
    }
}

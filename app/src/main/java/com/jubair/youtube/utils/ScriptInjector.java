package com.jubair.youtube.utils;

public class ScriptInjector {
    public static String getInjectScript() {
        return "javascript:(function() {" +
                // 1. CSS Injection (Native Look & Ad Hide)
                "var style = document.createElement('style');" +
                "style.innerHTML = '" +
                // টপ বার এবং "Open App" বাটন রিমুভ
                ".mobile-topbar-header, .pivot-bar-renderer, .ytm-app-upsell { display: none !important; }" +
                // ভিডিও এবং ব্যানার অ্যাড রিমুভ (GoodTube Logic)
                ".ad-container, .ad-interrupting, .video-ads, .ytp-ad-overlay-container { display: none !important; }" +
                ".ytm-promoted-sparkles-web-renderer, ytm-statement-banner-renderer { display: none !important; }" +
                // অতিরিক্ত স্পেস রিমুভ করে ফুলস্ক্রিন ফিল দেওয়া
                "body { background-color: #000000 !important; }" +
                "#player-container-id { position: fixed !important; top: 0 !important; width: 100% !important; z-index: 9999 !important; }" +
                "';" +
                "document.head.appendChild(style);" +

                // 2. JS Auto-Skip & Background Fix
                "setInterval(function(){" +
                "   try {" +
                "       var skipBtn = document.querySelector('.ytp-ad-skip-button');" +
                "       if(skipBtn) skipBtn.click();" +
                "       var overlayClose = document.querySelector('.ytp-ad-overlay-close-button');" +
                "       if(overlayClose) overlayClose.click();" +
                "       " +
                "       // ভিডিও যাতে পজ না হয় (ব্যাকগ্রাউন্ড প্লে হ্যাক)" +
                "       var video = document.querySelector('video');" +
                "       if(video) {" +
                "           video.setAttribute('playsinline', 'true');" +
                "           if(video.paused && !document.hidden && video.currentTime > 0) video.play();" +
                "       }" +
                "   } catch(e) {}" +
                "}, 1000);" +
                "})()";
    }
}

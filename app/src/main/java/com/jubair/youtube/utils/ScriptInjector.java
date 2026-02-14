package com.jubair.youtube.utils;

public class ScriptInjector {
    public static String getInjectScript() {
        return "javascript:(function() {" +
                // 1. CSS Injection (Native Look & Real PiP Fix)
                "var style = document.createElement('style');" +
                "style.innerHTML = '" +
                
                // --- HARDCORE AD BLOCKING ---
                ".ad-container, .ad-interrupting, .video-ads, .ytp-ad-overlay-container { display: none !important; }" +
                ".ytm-promoted-sparkles-web-renderer, ytm-statement-banner-renderer { display: none !important; }" +
                ".ytm-app-upsell, [aria-label=\"Open App\"] { display: none !important; }" +

                // --- UI CLEANUP (Header/Footer Removal) ---
                "ytm-mobile-topbar-renderer { display: none !important; }" +
                ".mobile-topbar-header, #header-bar { display: none !important; }" +
                "ytm-pivot-bar-renderer, .pivot-bar-renderer { display: none !important; }" +
                "ytm-search { display: none !important; }" + // সার্চ বার হাইড

                // --- REAL PiP FIX (Video Fullscreen Force) ---
                // ভিডিও প্লেয়ারকে জোর করে পুরো স্ক্রিনে বসানো হচ্ছে
                "body { background-color: #000000 !important; }" +
                "#player-container-id { position: fixed !important; top: 0 !important; left: 0 !important; width: 100vw !important; height: 100vh !important; z-index: 99999 !important; background: #000 !important; }" +
                ".player-container { height: 100% !important; }" +
                "video { object-fit: contain !important; width: 100% !important; height: 100% !important; }" +
                
                // কন্টেন্ট নিচে নামিয়ে দেওয়া যাতে ভিডিওর নিচে থাকে
                "ytm-browse, ytm-watch { padding-top: 0 !important; margin-top: 0 !important; }" +
                "';" +
                "document.head.appendChild(style);" +

                // 2. JS Auto-Skip & Background Audio Keep-Alive
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
                
                //          Media Session Update (Native Controls এর জন্য মেটাডেটা)
                "           if ('mediaSession' in navigator) {" +
                "               navigator.mediaSession.metadata = new MediaMetadata({" +
                "                   title: document.title.replace(' - YouTube', '')," +
                "                   artist: 'GoodTube Pro'," +
                "                   artwork: [{ src: 'https://www.youtube.com/img/desktop/yt_1200.png', sizes: '512x512', type: 'image/png' }]" +
                "               });" +
                "           }" +
                "       }" +
                "   } catch(e) {}" +
                "}, 1000);" +
                "})()";
    }
}

package com.jubair.youtube.utils;

public class ScriptInjector {
    public static String getInjectScript() {
        return "javascript:(function() {" +
                // --- 1. JSON HIJACKING (The Core AdBlock) ---
                // ইউটিউব প্লেয়ারের ডাটা মাঝপথে ধরে এড রিমুভ করা
                "var originalJSONParse = JSON.parse;" +
                "JSON.parse = function(text) {" +
                "   var data = originalJSONParse(text);" +
                "   if (data.adPlacements) { delete data.adPlacements; }" +
                "   if (data.playerAds) { delete data.playerAds; }" +
                "   if (data.adSlots) { delete data.adSlots; }" +
                "   return data;" +
                "};" +

                // --- 2. XHR/FETCH INTERCEPTOR ---
                // ব্যাকগ্রাউন্ড রিকোয়েস্ট থেকে এড ফিল্টার করা
                "var originalOpen = XMLHttpRequest.prototype.open;" +
                "XMLHttpRequest.prototype.open = function(method, url) {" +
                "   if (url.indexOf('/ad_') > -1 || url.indexOf('doubleclick') > -1) {" +
                "       // এড রিকোয়েস্ট হলে ফেক ইউআরএল এ পাঠাও" +
                "       return originalOpen.apply(this, [method, 'https://localhost/block']);" +
                "   }" +
                "   return originalOpen.apply(this, arguments);" +
                "};" +

                // --- 3. HARDCORE CSS (UI Cleanup) ---
                "var style = document.createElement('style');" +
                "style.innerHTML = '" +
                // সব ধরণের এড এলিমেন্ট হাইড
                ".ad-container, .ad-interrupting, .video-ads, .ytp-ad-overlay-container { display: none !important; }" +
                ".ytm-promoted-sparkles-web-renderer, ytm-statement-banner-renderer { display: none !important; }" +
                ".ytm-app-upsell, [aria-label=\"Open App\"] { display: none !important; }" +
                // হেডার, ফুটার এবং সার্চ বার হাইড (ভিডিও মোডে)
                "body.video-mode ytm-mobile-topbar-renderer { display: none !important; }" +
                "body.video-mode .mobile-topbar-header { display: none !important; }" +
                "body.video-mode ytm-pivot-bar-renderer { display: none !important; }" +
                // ভিডিও প্লেয়ার ফিক্স
                "body.video-mode #player-container-id { position: fixed !important; top: 0 !important; width: 100% !important; z-index: 99999 !important; background: #000 !important; }" +
                "';" +
                "document.head.appendChild(style);" +

                // --- 4. AUTO SKIPPER & VIDEO MODE DETECTOR ---
                "setInterval(function(){" +
                "   try {" +
                "       // ভিডিও মোড ডিটেকশন (UI হাইড করার জন্য)" +
                "       if (window.location.href.indexOf('/watch') > -1) {" +
                "           document.body.classList.add('video-mode');" +
                "       } else {" +
                "           document.body.classList.remove('video-mode');" +
                "       }" +

                "       // যদি কোনো এড লিক হয়ে যায়, সেটা ১ সেকেন্ডের ১০০০ ভাগের ১ ভাগে স্কিপ করা" +
                "       var skipBtn = document.querySelector('.ytp-ad-skip-button');" +
                "       if(skipBtn) skipBtn.click();" +
                "       var overlayClose = document.querySelector('.ytp-ad-overlay-close-button');" +
                "       if(overlayClose) overlayClose.click();" +
                "       " +
                "       // ভিডিও এড জোর করে ফাস্ট ফরোয়ার্ড করা" +
                "       var video = document.querySelector('video');" +
                "       if(video && document.querySelector('.ad-interrupting')) {" +
                "           video.currentTime = video.duration;" +
                "           video.click();" +
                "       }" +
                
                "       // ব্যাকগ্রাউন্ড প্লে হ্যাক" +
                "       if(video && video.paused && !document.hidden && video.currentTime > 0) {" +
                "            video.play();" +
                "       }" +
                "   } catch(e) {}" +
                "}, 100);" + // প্রতি ১০০ms পর পর চেক করবে
                "})()";
    }
}

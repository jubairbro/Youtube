package com.jubair.youtube.utils;

public class ScriptInjector {
    public static String getRemoveAdsScript() {
        return "javascript:(function() {" +
                "var style = document.createElement('style');" +
                "style.innerHTML = '" +
                // ইন্টারফেস ক্লিন করা
                ".mobile-topbar-header, .pivot-bar-renderer { display: none !important; }" +
                ".ytm-app-upsell, [aria-label=\"Open App\"] { display: none !important; }" +
                
                // ভিডিও অ্যাড এবং ব্যানার অ্যাড রিমুভ
                ".ad-container, .ad-interrupting, .video-ads { display: none !important; }" +
                ".ytm-promoted-sparkles-web-renderer { display: none !important; }" +
                ".ytm-statement-banner-renderer { display: none !important; }" +
                
                // হোম পেজের ফালতু শর্টস রিমুভ (অপশনাল)
                // "ytm-reel-shelf-renderer { display: none !important; }" +
                "';" +
                "document.head.appendChild(style);" +
                
                // ভিডিওর মাঝখানের অ্যাড স্কিপ করার চেষ্টা
                "setInterval(function(){" +
                "   var skipBtn = document.querySelector('.ytp-ad-skip-button');" +
                "   if(skipBtn) skipBtn.click();" +
                "   var adOverlay = document.querySelector('.ytp-ad-overlay-close-button');" +
                "   if(adOverlay) adOverlay.click();" +
                "}, 1000);" +
                "})()";
    }
}

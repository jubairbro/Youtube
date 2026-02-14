package com.jubair.youtube.utils;

public class GoodTubeScript {
    public static String getInjectScript() {
        // এই স্ট্রিংটি ডিএক্সে এনকোডেড হয়ে থাকবে
        return "javascript:(function() {" +
                "Object.defineProperty(document, 'hidden', { value: false });" +
                "var style = document.createElement('style');" +
                "style.innerHTML = '.ad-container, .ytm-app-upsell, .video-ads { display: none !important; }';" +
                "document.head.appendChild(style);" +
                "document.addEventListener('play', function(e){ if(e.target.tagName==='VIDEO') Android.onVideoPlay(); }, true);" +
                "document.addEventListener('pause', function(e){ if(e.target.tagName==='VIDEO') Android.onVideoPause(); }, true);" +
                "setInterval(function(){ " +
                "  var v = document.querySelector('video'); if(v) v.setAttribute('playsinline', 'true');" +
                "  var s = document.querySelector('.ytp-ad-skip-button'); if(s) s.click();" +
                "}, 1000);" +
                "})()";
    }
}

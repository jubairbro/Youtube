package com.jubair.youtube.utils;

public class ScriptInjector {
    public static String getGoodTubeScript() {
        // এটি সরাসরি আপনার আপলোড করা goodtube.user.js এর লজিক
        // এটি গিটহাব থেকে লেটেস্ট GoodTube স্ক্রিপ্ট লোড করবে
        return "javascript:(function() {" +
                "   function goodTube_load(loadAttempts) {" +
                "       loadAttempts = loadAttempts || 0;" +
                "       if (loadAttempts >= 10) return;" +
                "       var script = document.createElement('script');" +
                "       script.src = 'https://raw.githubusercontent.com/goodtube4u/goodtube/refs/heads/main/goodtube.min.js';" +
                "       script.onload = function() { console.log('GoodTube Loaded!'); };" +
                "       script.onerror = function() {" +
                "           setTimeout(function() { goodTube_load(loadAttempts + 1); }, 500);" +
                "       };" +
                "       document.head.appendChild(script);" +
                "   }" +
                "   goodTube_load();" +
                "})()";
    }
}

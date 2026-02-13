package com.jubair.youtube.utils;

public class ScriptInjector {
    public static String getRemoveAdsScript() {
        return "javascript:(function() {" +
                "var style = document.createElement('style');" +
                "style.innerHTML = '" +
                // "Open App" বাটন এবং হেডার রিমুভ (agressive)
                ".mobile-topbar-header { display: none !important; }" + 
                ".pivot-bar-renderer { display: none !important; }" +
                "ytm-app-upsell { display: none !important; }" +
                "[aria-label=\"Open App\"] { display: none !important; }" + // Attribute selector
                ".open-app-button { display: none !important; }" +
                
                // Login Prompts and Ads
                "ytm-statement-banner-renderer { display: none !important; }" +
                ".ad-container { display: none !important; }" +
                ".promo-message { display: none !important; }" +
                "';" +
                "document.head.appendChild(style);" +
                "})()";
    }
}

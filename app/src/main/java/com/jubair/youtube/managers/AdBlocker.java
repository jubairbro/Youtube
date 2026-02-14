package com.jubair.youtube.managers;

import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

public class AdBlocker {
    private static final Set<String> BLOCK_LIST = new HashSet<>();

    static {
        // Core Ads
        BLOCK_LIST.add("googleads.g.doubleclick.net");
        BLOCK_LIST.add("pagead2.googlesyndication.com");
        BLOCK_LIST.add("pubads.g.doubleclick.net");
        BLOCK_LIST.add("securepubads.g.doubleclick.net");
        
        // YouTube Specific Ad APIs
        BLOCK_LIST.add("youtube.com/api/stats/ads");
        BLOCK_LIST.add("youtube.com/ptracking");
        BLOCK_LIST.add("youtube.com/pagead");
        BLOCK_LIST.add("youtube.com/get_midroll_info");
        
        // Analytics & Tracking (এগুলো ব্লক করলে সাইট ফাস্ট হয়)
        BLOCK_LIST.add("static.doubleclick.net");
        BLOCK_LIST.add("yt3.ggpht.com/a-");
        BLOCK_LIST.add("jnn-pa.googleapis.com");
        BLOCK_LIST.add("google-analytics.com");
        BLOCK_LIST.add("googletagservices.com");
    }

    public static boolean isAd(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();
        
        // ১. হোস্ট লেভেল ব্লকিং
        for (String block : BLOCK_LIST) {
            if (lowerUrl.contains(block)) return true;
        }
        
        // ২. ডাইনামিক প্যাটার্ন ব্লকিং
        if (lowerUrl.contains("/pagead/") || 
            lowerUrl.contains("doubleclick.net") ||
            (lowerUrl.contains("youtube.com") && lowerUrl.contains("ad_format"))) {
            return true;
        }
        
        return false;
    }

    public static WebResourceResponse createEmptyResponse() {
        // একদম খালি রেসপন্স (নেটওয়ার্ক সাশ্রয় হবে)
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
    }
}

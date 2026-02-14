package com.jubair.youtube.managers;

import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

public class AdBlocker {
    private static final Set<String> AD_HOSTS = new HashSet<>();

    static {
        // গুডটিউব এবং অন্যান্য সোর্স থেকে সংগৃহীত অ্যাড সার্ভার লিস্ট
        AD_HOSTS.add("googleads.g.doubleclick.net");
        AD_HOSTS.add("pagead2.googlesyndication.com");
        AD_HOSTS.add("pubads.g.doubleclick.net");
        AD_HOSTS.add("youtube.com/api/stats/ads");
        AD_HOSTS.add("youtube.com/ptracking");
        AD_HOSTS.add("youtube.com/pagead");
        AD_HOSTS.add("google.com/pagead");
        AD_HOSTS.add("static.doubleclick.net");
        AD_HOSTS.add("yt3.ggpht.com/a-"); // ট্র্যাকিং পিক্সেল
    }

    public static boolean isAd(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();
        
        // ১. হোস্ট ম্যাচিং
        for (String host : AD_HOSTS) {
            if (lowerUrl.contains(host)) return true;
        }
        
        // ২. কিওয়ার্ড ম্যাচিং (ভিডিও অ্যাড এবং ব্যানার)
        if (lowerUrl.contains("/ad_status") || 
            lowerUrl.contains("/ads/") || 
            lowerUrl.contains("doubleclick") || 
            lowerUrl.contains("&ad_type=")) {
            return true;
        }
        
        return false;
    }

    public static WebResourceResponse createEmptyResponse() {
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
    }
}

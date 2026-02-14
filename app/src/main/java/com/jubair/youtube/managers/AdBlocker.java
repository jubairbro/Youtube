package com.jubair.youtube.managers;

import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

public class AdBlocker {
    private static final Set<String> AD_SERVERS = new HashSet<>();

    static {
        // কোর অ্যাড সার্ভার
        AD_SERVERS.add("googleads.g.doubleclick.net");
        AD_SERVERS.add("pagead2.googlesyndication.com");
        AD_SERVERS.add("pubads.g.doubleclick.net");
        AD_SERVERS.add("youtube.com/api/stats/ads");
        AD_SERVERS.add("youtube.com/ptracking");
        AD_SERVERS.add("youtube.com/pagead");
    }

    public static boolean isAd(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();

        // 1. googlevideo.com থেকে ভিডিও অ্যাড ডিটেকশন
        if (lowerUrl.contains("googlevideo.com")) {
            // যদি ইউআরএল এর মধ্যে 'ad_format' থাকে, তবে সেটা ভিডিও অ্যাড
            if (lowerUrl.contains("ad_format") || lowerUrl.contains("ctier")) {
                return true; 
            }
            // আসল ভিডিও এলাউ করা হবে
            return false;
        }

        // 2. সাধারণ ব্যানার অ্যাড ব্লক
        for (String server : AD_SERVERS) {
            if (lowerUrl.contains(server)) return true;
        }

        return false;
    }

    public static WebResourceResponse createEmptyResponse() {
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
    }
}

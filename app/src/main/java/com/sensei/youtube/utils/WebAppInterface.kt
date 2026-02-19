package com.sensei.youtube.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import com.sensei.youtube.services.MusicPlayerService

class WebAppInterface(private val context: Context) {
    
    companion object {
        private const val TAG = "WebAppInterface"
        const val INTERFACE_NAME = "AndroidInterface"
        
        fun injectJavaScript(webView: WebView) {
            val js = """
                (function() {
                    'use strict';
                    
                    if (window.__youtubeInterfaceInjected) {
                        console.log('Interface already injected');
                        return;
                    }
                    window.__youtubeInterfaceInjected = true;
                    
                    console.log('Injecting YouTube interface...');
                    
                    // Video element tracking
                    var currentVideoId = null;
                    var currentVideoTitle = '';
                    var currentChannelName = '';
                    
                    // Extract video ID from URL
                    function getVideoId() {
                        var url = window.location.href;
                        var match = url.match(/[?&]v=([^&]+)/);
                        if (match) return match[1];
                        
                        // Try /watch/ID or /live/ID format
                        match = url.match(/\/(watch|live)\/([^?&]+)/);
                        if (match) return match[2];
                        
                        return null;
                    }
                    
                    // Get video title
                    function getVideoTitle() {
                        var selectors = [
                            'h1.ytd-video-primary-info-renderer',
                            'h1.title',
                            '#title h1',
                            'ytd-watch-metadata h1',
                            'h1.ytd-watch-metadata'
                        ];
                        
                        for (var i = 0; i < selectors.length; i++) {
                            var el = document.querySelector(selectors[i]);
                            if (el && el.textContent) {
                                return el.textContent.trim();
                            }
                        }
                        return document.title.replace(' - YouTube', '').trim();
                    }
                    
                    // Get channel name
                    function getChannelName() {
                        var selectors = [
                            'ytd-channel-name a',
                            '#channel-name a',
                            'a.yt-simple-endpoint.ytd-channel-name',
                            'ytd-video-owner-renderer a'
                        ];
                        
                        for (var i = 0; i < selectors.length; i++) {
                            var el = document.querySelector(selectors[i]);
                            if (el && el.textContent) {
                                return el.textContent.trim();
                            }
                        }
                        return 'Unknown';
                    }
                    
                    // Get video element
                    function getVideoElement() {
                        return document.querySelector('video.html5-main-video') || 
                               document.querySelector('#movie_player video') ||
                               document.querySelector('video');
                    }
                    
                    // Notify Android about video state
                    function notifyVideoState() {
                        var video = getVideoElement();
                        if (!video) return;
                        
                        var videoId = getVideoId();
                        var title = getVideoTitle();
                        var channel = getChannelName();
                        
                        if (window.AndroidInterface) {
                            if (videoId !== currentVideoId) {
                                currentVideoId = videoId;
                                currentVideoTitle = title;
                                currentChannelName = channel;
                                window.AndroidInterface.onVideoChanged(videoId || '', title, channel);
                            }
                            
                            if (!video.paused) {
                                window.AndroidInterface.onVideoPlaying();
                            } else {
                                window.AndroidInterface.onVideoPaused();
                            }
                        }
                    }
                    
                    // Setup video listeners
                    function setupVideoListeners() {
                        var video = getVideoElement();
                        if (!video) return false;
                        
                        // Remove old listeners by cloning
                        var newVideo = video.cloneNode(true);
                        video.parentNode.replaceChild(newVideo, video);
                        video = newVideo;
                        
                        video.addEventListener('play', function() {
                            console.log('Video playing');
                            notifyVideoState();
                        });
                        
                        video.addEventListener('pause', function() {
                            console.log('Video paused');
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onVideoPaused();
                            }
                        });
                        
                        video.addEventListener('ended', function() {
                            console.log('Video ended');
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onVideoEnded();
                            }
                        });
                        
                        video.addEventListener('timeupdate', function() {
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onTimeUpdate(video.currentTime, video.duration || 0);
                            }
                        });
                        
                        // Initial state
                        if (!video.paused) {
                            notifyVideoState();
                        }
                        
                        return true;
                    }
                    
                    // Try to setup listeners
                    function trySetupListeners() {
                        if (!setupVideoListeners()) {
                            setTimeout(trySetupListeners, 500);
                        }
                    }
                    
                    // Start
                    trySetupListeners();
                    
                    // Watch for video changes
                    var lastUrl = window.location.href;
                    setInterval(function() {
                        if (window.location.href !== lastUrl) {
                            lastUrl = window.location.href;
                            console.log('URL changed:', lastUrl);
                            setTimeout(trySetupListeners, 1000);
                        }
                        notifyVideoState();
                    }, 1000);
                    
                    // Mutation observer for dynamic content
                    var observer = new MutationObserver(function(mutations) {
                        mutations.forEach(function(mutation) {
                            if (mutation.addedNodes.length > 0) {
                                mutation.addedNodes.forEach(function(node) {
                                    if (node.tagName === 'VIDEO' || 
                                        (node.querySelector && node.querySelector('video'))) {
                                        setTimeout(setupVideoListeners, 100);
                                    }
                                });
                            }
                        });
                    });
                    
                    observer.observe(document.body, {
                        childList: true,
                        subtree: true
                    });
                    
                    console.log('YouTube interface injected successfully');
                })();
            """.trimIndent()
            
            webView.evaluateJavascript(js) { result ->
                Log.d(TAG, "Interface injection result: $result")
            }
        }
    }
    
    @JavascriptInterface
    fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    @JavascriptInterface
    fun onVideoPlaying() {
        Log.d(TAG, "Video playing")
    }
    
    @JavascriptInterface
    fun onVideoPaused() {
        Log.d(TAG, "Video paused")
    }
    
    @JavascriptInterface
    fun onVideoEnded() {
        Log.d(TAG, "Video ended")
    }
    
    @JavascriptInterface
    fun onVideoChanged(videoId: String, title: String, channel: String) {
        Log.d(TAG, "Video changed: $videoId - $title")
        MusicPlayerService.currentVideoTitle = title
        MusicPlayerService.currentVideoAuthor = channel
    }
    
    @JavascriptInterface
    fun onTimeUpdate(currentTime: Double, duration: Double) {
        // Progress update
    }
}

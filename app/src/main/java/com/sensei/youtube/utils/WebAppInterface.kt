package com.sensei.youtube.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import com.sensei.youtube.services.NotificationService

class WebAppInterface(private val context: Context) {
    
    @JavascriptInterface
    fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    @JavascriptInterface
    fun getVideoTitle(): String {
        return NotificationService.currentVideoTitle ?: "YouTube Lite"
    }
    
    @JavascriptInterface
    fun getVideoAuthor(): String {
        return NotificationService.currentVideoAuthor ?: "Unknown"
    }
    
    @JavascriptInterface
    fun onVideoPlaying() {
        Handler(Looper.getMainLooper()).post {
            NotificationService.updatePlaybackState(context, true)
        }
    }
    
    @JavascriptInterface
    fun onVideoPaused() {
        Handler(Looper.getMainLooper()).post {
            NotificationService.updatePlaybackState(context, false)
        }
    }
    
    @JavascriptInterface
    fun onVideoInfo(title: String, author: String) {
        Handler(Looper.getMainLooper()).post {
            NotificationService.updateVideoInfo(context, title, author)
        }
    }
    
    companion object {
        const val INTERFACE_NAME = "AndroidInterface"
        
        fun injectJavaScript(webView: WebView) {
            val js = """
                (function() {
                    if (window.AndroidInterfaceInjected) return;
                    window.AndroidInterfaceInjected = true;
                    
                    function getVideoElement() {
                        return document.querySelector('video.html5-main-video') || 
                               document.querySelector('video') ||
                               document.getElementsByTagName('video')[0];
                    }
                    
                    function getVideoTitle() {
                        var titleEl = document.querySelector('h1.title') ||
                                     document.querySelector('ytd-video-primary-info-renderer h1') ||
                                     document.querySelector('h1.ytd-video-primary-info-renderer') ||
                                     document.querySelector('#title h1') ||
                                     document.querySelector('title');
                        return titleEl ? titleEl.textContent.trim() : 'Unknown';
                    }
                    
                    function getChannelName() {
                        var channelEl = document.querySelector('ytd-channel-name a') ||
                                       document.querySelector('#channel-name a') ||
                                       document.querySelector('a.yt-simple-endpoint.ytd-channel-name');
                        return channelEl ? channelEl.textContent.trim() : 'Unknown';
                    }
                    
                    function notifyVideoInfo() {
                        var title = getVideoTitle();
                        var author = getChannelName();
                        if (window.AndroidInterface) {
                            window.AndroidInterface.onVideoInfo(title, author);
                        }
                    }
                    
                    function setupVideoListeners() {
                        var video = getVideoElement();
                        if (video) {
                            video.addEventListener('play', function() {
                                if (window.AndroidInterface) {
                                    window.AndroidInterface.onVideoPlaying();
                                    notifyVideoInfo();
                                }
                            });
                            
                            video.addEventListener('pause', function() {
                                if (window.AndroidInterface) {
                                    window.AndroidInterface.onVideoPaused();
                                }
                            });
                            
                            if (!video.paused) {
                                if (window.AndroidInterface) {
                                    window.AndroidInterface.onVideoPlaying();
                                    notifyVideoInfo();
                                }
                            }
                            
                            return true;
                        }
                        return false;
                    }
                    
                    function trySetup() {
                        if (!setupVideoListeners()) {
                            setTimeout(trySetup, 500);
                        }
                    }
                    
                    trySetup();
                    
                    var observer = new MutationObserver(function() {
                        setupVideoListeners();
                    });
                    
                    observer.observe(document.body, {
                        childList: true,
                        subtree: true
                    });
                })();
            """.trimIndent()
            
            webView.evaluateJavascript(js, null)
        }
    }
}

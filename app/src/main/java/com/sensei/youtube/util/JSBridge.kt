package com.sensei.youtube.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.sensei.youtube.service.AudioService

class JSBridge(private val ctx: Context) {
    
    companion object {
        private const val TAG = "JSBridge"
        const val NAME = "Android"
        
        fun inject(wv: WebView) {
            val js = """
                (function(){
                    if(window._yti)return;
                    window._yti=1;
                    console.log('[YTLite] Bridge v3 - Audio Fixed');
                    
                    var videoId=null;
                    var title='';
                    var channel='';
                    var lastUrl=location.href;
                    
                    function getId(){
                        var m=location.href.match(/[?&]v=([^&]+)/);
                        if(m)return m[1];
                        m=location.href.match(/\/(watch|live|shorts)\/([^?&#]+)/);
                        return m?m[2]:null;
                    }
                    
                    function getTitle(){
                        var el=document.querySelector('h1.ytd-video-primary-info-renderer')||
                                document.querySelector('h1.title')||
                                document.querySelector('#title h1')||
                                document.querySelector('ytd-watch-metadata h1')||
                                document.querySelector('yt-formatted-string.ytd-watch-metadata');
                        if(el && el.textContent)return el.textContent.trim();
                        return document.title.replace(' - YouTube','').trim();
                    }
                    
                    function getChannel(){
                        var el=document.querySelector('ytd-channel-name a')||
                                document.querySelector('#channel-name a')||
                                document.querySelector('a.yt-simple-endpoint.ytd-channel-name')||
                                document.querySelector('ytd-video-owner-renderer a');
                        return el?el.textContent.trim():'Unknown';
                    }
                    
                    function getVideo(){
                        return document.querySelector('video.html5-main-video')||
                               document.querySelector('#movie_player video')||
                               document.querySelector('video');
                    }
                    
                    // Extract video URL from video element
                    function getVideoUrl(){
                        var v=getVideo();
                        if(!v)return null;
                        
                        // Direct src
                        if(v.src && v.src.startsWith('http')){
                            return v.src;
                        }
                        
                        // currentSrc
                        if(v.currentSrc && v.currentSrc.startsWith('http')){
                            return v.currentSrc;
                        }
                        
                        // Try to get from blob
                        if(v.src && v.src.startsWith('blob:')){
                            // Look for googlevideo URL in page
                            var scripts=document.querySelectorAll('script');
                            for(var i=0;i<scripts.length;i++){
                                var txt=scripts[i].textContent||'';
                                var m=txt.match(/(https?:[^"'\s]+googlevideo[^"'\s]+)/);
                                if(m){
                                    var url=m[1];
                                    url=url.replace(/\\u0026/g,'&');
                                    url=url.replace(/\\\\/g,'');
                                    url=url.replace(/\\"/g,'"');
                                    if(url.includes('mime=video')||url.includes('mime=audio')){
                                        return decodeURIComponent(url);
                                    }
                                }
                            }
                        }
                        
                        return null;
                    }
                    
                    // Notify video info
                    function notifyVideo(){
                        var id=getId();
                        if(id && id!==videoId){
                            videoId=id;
                            title=getTitle();
                            channel=getChannel();
                            
                            if(window.Android){
                                Android.onVideoInfo(id,title,channel);
                            }
                        }
                    }
                    
                    // Setup video listeners
                    function setupVideo(){
                        var v=getVideo();
                        if(!v)return false;
                        
                        console.log('[YTLite] Video found, setting up listeners');
                        
                        v.addEventListener('play',function(){
                            console.log('[YTLite] Video playing');
                            notifyVideo();
                            
                            var url=getVideoUrl();
                            console.log('[YTLite] Video URL: '+(url?url.substring(0,50):'null'));
                            
                            if(url && window.Android){
                                Android.onStreamUrl(url,getTitle(),getChannel());
                            }
                            
                            if(window.Android){
                                Android.onPlaying();
                            }
                        });
                        
                        v.addEventListener('pause',function(){
                            console.log('[YTLite] Video paused');
                            if(window.Android)Android.onPaused();
                        });
                        
                        v.addEventListener('ended',function(){
                            console.log('[YTLite] Video ended');
                            if(window.Android)Android.onEnded();
                        });
                        
                        // If already playing
                        if(!v.paused){
                            notifyVideo();
                            var url=getVideoUrl();
                            if(url && window.Android){
                                Android.onStreamUrl(url,getTitle(),getChannel());
                            }
                        }
                        
                        notifyVideo();
                        return true;
                    }
                    
                    function trySetup(){
                        if(!setupVideo()){
                            setTimeout(trySetup,300);
                        }
                    }
                    
                    trySetup();
                    
                    // URL change
                    setInterval(function(){
                        if(location.href!==lastUrl){
                            lastUrl=location.href;
                            videoId=null;
                            setTimeout(trySetup,500);
                        }
                    },500);
                    
                    // Observer
                    new MutationObserver(function(){
                        if(!getVideo())trySetup();
                    }).observe(document.body,{childList:true,subtree:true});
                    
                    console.log('[YTLite] Bridge v3 ready');
                })();
            """.trimIndent()
            
            wv.evaluateJavascript(js) { Log.d(TAG, "Bridge: $it") }
        }
    }
    
    @JavascriptInterface
    fun onVideoInfo(id: String, t: String, c: String) {
        Log.d(TAG, "Video: $id - $t")
        AudioService.title = t
        AudioService.author = c
    }
    
    @JavascriptInterface
    fun onStreamUrl(url: String, title: String, channel: String) {
        Log.d(TAG, "Stream URL detected: ${url.take(80)}...")
        
        AudioService.title = title
        AudioService.author = channel
        AudioService.setPlaying(ctx, true, title, channel)
    }
    
    @JavascriptInterface
    fun onPlaying() {
        Log.d(TAG, "Playing")
    }
    
    @JavascriptInterface
    fun onPaused() {
        Log.d(TAG, "Paused")
    }
    
    @JavascriptInterface
    fun onEnded() {
        Log.d(TAG, "Ended")
    }
    
    @JavascriptInterface
    fun log(msg: String) {
        Log.d(TAG, "JS: $msg")
    }
}

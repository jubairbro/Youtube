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
                    console.log('[YTLite] Bridge injected');
                    
                    var videoId=null;
                    var title='';
                    var channel='';
                    
                    function getId(){
                        var m=location.href.match(/[?&]v=([^&]+)/);
                        if(m)return m[1];
                        m=location.href.match(/\/(watch|live|shorts)\/([^?&]+)/);
                        return m?m[2]:null;
                    }
                    
                    function getTitle(){
                        var el=document.querySelector('h1.ytd-video-primary-info-renderer')||
                                document.querySelector('h1.title')||
                                document.querySelector('#title h1')||
                                document.querySelector('ytd-watch-metadata h1');
                        return el?el.textContent.trim():document.title.replace(' - YouTube','').trim();
                    }
                    
                    function getChannel(){
                        var el=document.querySelector('ytd-channel-name a')||
                                document.querySelector('#channel-name a')||
                                document.querySelector('a.yt-simple-endpoint.ytd-channel-name');
                        return el?el.textContent.trim():'';
                    }
                    
                    function getVideo(){
                        return document.querySelector('video.html5-main-video')||
                               document.querySelector('#movie_player video')||
                               document.querySelector('video');
                    }
                    
                    // Find stream URL
                    function findStreamUrl(){
                        var video=getVideo();
                        if(video && video.src && video.src.startsWith('http')){
                            return video.src;
                        }
                        
                        // Check blob URLs
                        if(video && video.src && video.src.startsWith('blob:')){
                            // Try to find in page scripts
                            var scripts=document.querySelectorAll('script');
                            for(var i=0;i<scripts.length;i++){
                                var txt=scripts[i].textContent;
                                var m=txt.match(/"url":"([^"]+googlevideo[^"]+)"/);
                                if(m){
                                    return m[1].replace(/\\u0026/g,'&').replace(/\\/g,'');
                                }
                            }
                        }
                        
                        // Check video element currentSrc
                        if(video && video.currentSrc){
                            return video.currentSrc;
                        }
                        
                        return null;
                    }
                    
                    function notifyState(){
                        var v=getVideo();
                        if(!v)return;
                        
                        var id=getId();
                        if(id && id!==videoId){
                            videoId=id;
                            title=getTitle();
                            channel=getChannel();
                            if(window.Android){
                                Android.onVideo(id,title,channel);
                            }
                        }
                        
                        if(window.Android){
                            if(!v.paused){
                                Android.onPlay();
                            }else{
                                Android.onPause();
                            }
                        }
                    }
                    
                    // Watch video state
                    function setupVideo(){
                        var v=getVideo();
                        if(!v)return false;
                        
                        v.addEventListener('play',function(){
                            var url=findStreamUrl();
                            if(url && window.Android){
                                Android.onStream(url,getTitle(),getChannel());
                            }
                            notifyState();
                        });
                        
                        v.addEventListener('pause',function(){
                            notifyState();
                        });
                        
                        v.addEventListener('ended',function(){
                            if(window.Android)Android.onEnd();
                        });
                        
                        notifyState();
                        return true;
                    }
                    
                    function trySetup(){
                        if(!setupVideo()){
                            setTimeout(trySetup,500);
                        }
                    }
                    
                    trySetup();
                    
                    // URL change detection
                    var lastUrl=location.href;
                    setInterval(function(){
                        if(location.href!==lastUrl){
                            lastUrl=location.href;
                            videoId=null;
                            setTimeout(trySetup,1000);
                        }
                        notifyState();
                    },1000);
                    
                    // Mutation observer
                    new MutationObserver(function(){
                        setupVideo();
                    }).observe(document.body,{childList:true,subtree:true});
                    
                    console.log('[YTLite] Bridge ready');
                })();
            """.trimIndent()
            
            wv.evaluateJavascript(js) { Log.d(TAG, "Bridge: $it") }
        }
    }
    
    @JavascriptInterface
    fun onVideo(id: String, t: String, c: String) {
        Log.d(TAG, "Video: $id - $t")
        AudioService.title = t
        AudioService.author = c
    }
    
    @JavascriptInterface
    fun onStream(url: String, t: String, c: String) {
        Log.d(TAG, "Stream: ${url.take(50)}...")
        AudioService.play(ctx, url, t, c)
    }
    
    @JavascriptInterface
    fun onPlay() {
        Log.d(TAG, "Playing")
    }
    
    @JavascriptInterface
    fun onPause() {
        Log.d(TAG, "Paused")
    }
    
    @JavascriptInterface
    fun onEnd() {
        Log.d(TAG, "Ended")
    }
    
    @JavascriptInterface
    fun toast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

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
                    console.log('[YTLite] Bridge v2');
                    
                    var videoId=null;
                    var title='';
                    var channel='';
                    var streamUrl=null;
                    var isAudioOnly=false;
                    
                    // Get video ID from URL
                    function getId(){
                        var m=location.href.match(/[?&]v=([^&]+)/);
                        if(m)return m[1];
                        m=location.href.match(/\/(watch|live|shorts)\/([^?&#]+)/);
                        return m?m[2]:null;
                    }
                    
                    // Get video title
                    function getTitle(){
                        var selectors=[
                            'h1.ytd-video-primary-info-renderer',
                            'h1.title',
                            '#title h1',
                            'ytd-watch-metadata h1',
                            'yt-formatted-string.ytd-watch-metadata'
                        ];
                        for(var i=0;i<selectors.length;i++){
                            var el=document.querySelector(selectors[i]);
                            if(el && el.textContent)return el.textContent.trim();
                        }
                        return document.title.replace(' - YouTube','').trim();
                    }
                    
                    // Get channel name
                    function getChannel(){
                        var selectors=[
                            'ytd-channel-name a',
                            '#channel-name a',
                            'a.yt-simple-endpoint.ytd-channel-name',
                            'ytd-video-owner-renderer a'
                        ];
                        for(var i=0;i<selectors.length;i++){
                            var el=document.querySelector(selectors[i]);
                            if(el && el.textContent)return el.textContent.trim();
                        }
                        return '';
                    }
                    
                    // Get video element
                    function getVideo(){
                        return document.querySelector('video.html5-main-video')||
                               document.querySelector('#movie_player video')||
                               document.querySelector('video');
                    }
                    
                    // Extract audio stream URL from YouTube player
                    function extractAudioUrl(){
                        var video=getVideo();
                        if(!video)return null;
                        
                        // Method 1: Direct video src
                        if(video.src && video.src.startsWith('http')){
                            return video.src;
                        }
                        
                        // Method 2: currentSrc
                        if(video.currentSrc && video.currentSrc.startsWith('http')){
                            return video.currentSrc;
                        }
                        
                        // Method 3: Extract from YouTube player config
                        try{
                            var player=document.querySelector('#movie_player')||document.querySelector('.html5-video-player');
                            if(player && player.getPlayerResponse){
                                var resp=player.getPlayerResponse();
                                if(resp && resp.streamingData){
                                    // Try adaptive formats (audio only)
                                    var adaptive=resp.streamingData.adaptiveFormats;
                                    if(adaptive){
                                        for(var i=0;i<adaptive.length;i++){
                                            var fmt=adaptive[i];
                                            if(fmt.mimeType && fmt.mimeType.startsWith('audio/')){
                                                return fmt.url;
                                            }
                                        }
                                    }
                                    // Fallback to regular formats
                                    var formats=resp.streamingData.formats;
                                    if(formats && formats[0]){
                                        return formats[0].url;
                                    }
                                }
                            }
                        }catch(e){console.log('Player extract error:',e)}
                        
                        // Method 4: Search in page scripts for googlevideo URL
                        var scripts=document.querySelectorAll('script');
                        for(var i=0;i<scripts.length;i++){
                            var txt=scripts[i].textContent||scripts[i].innerHTML;
                            
                            // Look for url encoded in JSON
                            var matches=txt.match(/"url"\s*:\s*"(https?:[^"]+googlevideo[^"]+)"/g);
                            if(matches){
                                for(var j=0;j<matches.length;j++){
                                    var url=matches[j].replace(/^"url"\s*:\s*"/,'').replace(/"$/,'');
                                    url=url.replace(/\\u0026/g,'&').replace(/\\\\/g,'').replace(/\\//g,'/');
                                    if(url.includes('audio')){
                                        return url;
                                    }
                                }
                                // Return first found if no audio specific
                                var url=matches[0].replace(/^"url"\s*:\s*"/,'').replace(/"$/,'');
                                return url.replace(/\\u0026/g,'&').replace(/\\\\/g,'').replace(/\\//g,'/');
                            }
                        }
                        
                        return null;
                    }
                    
                    // Notify Android of state changes
                    function notifyState(){
                        var v=getVideo();
                        if(!v)return;
                        
                        var id=getId();
                        if(id && id!==videoId){
                            videoId=id;
                            title=getTitle();
                            channel=getChannel();
                            streamUrl=extractAudioUrl();
                            
                            if(window.Android){
                                Android.onVideo(id,title,channel);
                            }
                        }
                        
                        if(window.Android){
                            var time=v.currentTime||0;
                            var dur=v.duration||0;
                            Android.onState(!v.paused,time,dur);
                        }
                    }
                    
                    // Setup video listeners
                    function setupVideo(){
                        var v=getVideo();
                        if(!v)return false;
                        
                        // Remove old listeners
                        var newV=v.cloneNode(true);
                        v.parentNode.replaceChild(newV,v);
                        v=newV;
                        
                        v.addEventListener('play',function(){
                            title=getTitle();
                            channel=getChannel();
                            streamUrl=extractAudioUrl();
                            
                            if(window.Android){
                                Android.onPlay(title,channel);
                            }
                            
                            // Try to get audio URL for ExoPlayer
                            if(streamUrl && window.Android){
                                Android.onAudioUrl(streamUrl,title,channel);
                            }
                        });
                        
                        v.addEventListener('pause',function(){
                            if(window.Android)Android.onPause();
                        });
                        
                        v.addEventListener('ended',function(){
                            if(window.Android)Android.onEnded();
                        });
                        
                        v.addEventListener('timeupdate',function(){
                            if(window.Android){
                                Android.onTime(v.currentTime||0,v.duration||0);
                            }
                        });
                        
                        notifyState();
                        return true;
                    }
                    
                    function trySetup(){
                        if(!setupVideo()){
                            setTimeout(trySetup,300);
                        }
                    }
                    
                    trySetup();
                    
                    // URL change detection
                    var lastUrl=location.href;
                    setInterval(function(){
                        if(location.href!==lastUrl){
                            lastUrl=location.href;
                            videoId=null;
                            streamUrl=null;
                            setTimeout(trySetup,500);
                        }
                        notifyState();
                    },500);
                    
                    // Mutation observer
                    new MutationObserver(function(){
                        if(!getVideo())trySetup();
                    }).observe(document.body,{childList:true,subtree:true});
                    
                    console.log('[YTLite] Bridge ready v2');
                })();
            """.trimIndent()
            
            wv.evaluateJavascript(js) { Log.d(TAG, "Bridge injected: $it") }
        }
    }
    
    @JavascriptInterface
    fun onVideo(id: String, title: String, channel: String) {
        Log.d(TAG, "Video: $id")
        AudioService.title = title
        AudioService.author = channel
    }
    
    @JavascriptInterface
    fun onPlay(title: String, channel: String) {
        Log.d(TAG, "Playing: $title")
        AudioService.title = title
        AudioService.author = channel
    }
    
    @JavascriptInterface
    fun onPause() {
        Log.d(TAG, "Paused")
    }
    
    @JavascriptInterface
    fun onEnded() {
        Log.d(TAG, "Ended")
    }
    
    @JavascriptInterface
    fun onState(playing: Boolean, time: Double, duration: Double) {
        // State update
    }
    
    @JavascriptInterface
    fun onTime(current: Double, duration: Double) {
        // Time update
    }
    
    @JavascriptInterface
    fun onAudioUrl(url: String, title: String, channel: String) {
        Log.d(TAG, "Audio URL found: ${url.take(60)}...")
        // This URL can be passed to ExoPlayer for native audio playback
        AudioService.play(ctx, url, title, channel)
    }
    
    @JavascriptInterface
    fun toast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

package com.sensei.youtube.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.sensei.youtube.R
import com.sensei.youtube.ui.MainActivity

class AudioService : Service() {
    
    private var player: ExoPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    companion object {
        private const val TAG = "AudioService"
        const val CHANNEL_ID = "yt_playback"
        const val NOTIF_ID = 1001
        
        const val ACTION_PLAY = "play"
        const val ACTION_PAUSE = "pause"
        const val ACTION_STOP = "stop"
        const val ACTION_URL = "url"
        const val ACTION_UPDATE = "update"
        
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_AUTHOR = "author"
        
        var title: String = "YouTube Lite"
        var author: String = ""
        
        private var inst: AudioService? = null
        
        fun isPlaying(): Boolean = inst?.player?.isPlaying == true
        
        fun play() { 
            Log.d(TAG, "play()")
            inst?.player?.play() 
        }
        
        fun pause() { 
            Log.d(TAG, "pause()")
            inst?.player?.pause() 
        }
        
        fun play(ctx: Context, url: String, t: String = "", a: String = "") {
            Log.d(TAG, "play url: ${url.take(60)}...")
            title = t.ifEmpty { "YouTube Lite" }
            author = a
            
            val i = Intent(ctx, AudioService::class.java).apply {
                action = ACTION_URL
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, t)
                putExtra(EXTRA_AUTHOR, a)
            }
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(i)
                } else {
                    ctx.startService(i)
                }
            } catch (e: Exception) {
                Log.e(TAG, "start failed", e)
            }
        }
        
        fun stop(ctx: Context) {
            Log.d(TAG, "stop()")
            try {
                ctx.stopService(Intent(ctx, AudioService::class.java))
            } catch (e: Exception) {}
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        inst = this
        Log.d(TAG, "onCreate")
        
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "yt::audio")
        
        initPlayer()
    }
    
    private fun initPlayer() {
        Log.d(TAG, "initPlayer")
        
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10000)
            .setReadTimeoutMs(30000)
        
        player = ExoPlayer.Builder(this)
            .build()
            .apply {
                playWhenReady = true
            }
        
        Log.d(TAG, "Player initialized")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_PLAY -> {
                player?.play()
                updateNotif()
            }
            ACTION_PAUSE -> {
                player?.pause()
                updateNotif()
            }
            ACTION_STOP -> {
                player?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE -> {
                updateNotif()
            }
            ACTION_URL -> {
                val url = intent.getStringExtra(EXTRA_URL)
                val t = intent.getStringExtra(EXTRA_TITLE) ?: ""
                val a = intent.getStringExtra(EXTRA_AUTHOR) ?: ""
                
                title = t.ifEmpty { "YouTube Lite" }
                author = a
                
                Log.d(TAG, "URL received: ${url?.take(60)}...")
                
                if (!url.isNullOrEmpty()) {
                    startFg()
                    playUrl(url)
                    wakeLock?.acquire(30 * 60 * 1000L)
                }
            }
            else -> {
                startFg()
            }
        }
        
        return START_STICKY
    }
    
    private fun startFg() {
        val notif = buildNotif()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIF_ID, notif)
            }
            Log.d(TAG, "Foreground started")
        } catch (e: Exception) {
            Log.e(TAG, "startFg failed", e)
        }
    }
    
    private fun playUrl(url: String) {
        Log.d(TAG, "playUrl: ${url.take(80)}...")
        
        try {
            val uri = Uri.parse(url)
            val item = MediaItem.fromUri(uri)
            
            player?.apply {
                setMediaItem(item)
                prepare()
                playWhenReady = true
            }
            
            Log.d(TAG, "Playback started")
            updateNotif()
            
        } catch (e: Exception) {
            Log.e(TAG, "playUrl failed", e)
        }
    }
    
    private fun updateNotif() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID, buildNotif())
        } catch (e: Exception) {
            Log.e(TAG, "updateNotif failed", e)
        }
    }
    
    private fun buildNotif(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val playPi = PendingIntent.getService(
            this, 1,
            Intent(this, AudioService::class.java).setAction(ACTION_PLAY),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val pausePi = PendingIntent.getService(
            this, 2,
            Intent(this, AudioService::class.java).setAction(ACTION_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopPi = PendingIntent.getService(
            this, 3,
            Intent(this, AudioService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val playing = player?.isPlaying == true
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(author.ifEmpty { "Background Playback" })
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pi)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(playing)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(
                if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                if (playing) "Pause" else "Play",
                if (playing) pausePi else playPi
            )
            .addAction(R.drawable.ic_settings, "Stop", stopPi)
            .build()
    }
    
    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        player?.release()
        player = null
        wakeLock?.let { if (it.isHeld) it.release() }
        inst = null
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}

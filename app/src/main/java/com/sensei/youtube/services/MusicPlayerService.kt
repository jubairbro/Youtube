package com.sensei.youtube.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.sensei.youtube.R
import com.sensei.youtube.ui.MainActivity

class MusicPlayerService : Service() {
    
    private var player: ExoPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var notificationManager: NotificationManager? = null
    
    companion object {
        private const val TAG = "MusicPlayerService"
        const val CHANNEL_ID = "youtube_lite_playback"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_PLAY = "com.sensei.youtube.ACTION_PLAY"
        const val ACTION_PAUSE = "com.sensei.youtube.ACTION_PAUSE"
        const val ACTION_STOP = "com.sensei.youtube.ACTION_STOP"
        const val ACTION_SET_URL = "com.sensei.youtube.ACTION_SET_URL"
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_VIDEO_TITLE = "video_title"
        const val EXTRA_VIDEO_AUTHOR = "video_author"
        
        var currentVideoTitle: String? = null
        var currentVideoAuthor: String? = null
        var currentVideoUrl: String? = null
        
        private var instance: MusicPlayerService? = null
        
        fun isPlaying(): Boolean = instance?.player?.isPlaying ?: false
        
        fun play(context: Context) {
            instance?.player?.play()
        }
        
        fun pause(context: Context) {
            instance?.player?.pause()
        }
        
        fun setVideo(context: Context, url: String, title: String? = null, author: String? = null) {
            currentVideoUrl = url
            currentVideoTitle = title
            currentVideoAuthor = author
            
            val intent = Intent(context, MusicPlayerService::class.java).apply {
                action = ACTION_SET_URL
                putExtra(EXTRA_VIDEO_URL, url)
                putExtra(EXTRA_VIDEO_TITLE, title)
                putExtra(EXTRA_VIDEO_AUTHOR, author)
            }
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service", e)
            }
        }
    }
    
    private val binder = LocalBinder()
    
    inner class LocalBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "YouTubeLite::MusicPlayback"
        )
        
        initializePlayer()
    }
    
    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            playWhenReady = true
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                player?.play()
                updateNotification()
            }
            ACTION_PAUSE -> {
                player?.pause()
                updateNotification()
            }
            ACTION_STOP -> {
                player?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_SET_URL -> {
                val url = intent.getStringExtra(EXTRA_VIDEO_URL)
                val title = intent.getStringExtra(EXTRA_VIDEO_TITLE)
                val author = intent.getStringExtra(EXTRA_VIDEO_AUTHOR)
                
                currentVideoTitle = title
                currentVideoAuthor = author
                
                if (!url.isNullOrEmpty()) {
                    playVideo(url)
                }
                
                startForegroundNotification()
                wakeLock?.acquire(30 * 60 * 1000L)
            }
            else -> {
                startForegroundNotification()
            }
        }
        
        return START_STICKY
    }
    
    private fun playVideo(url: String) {
        try {
            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play video", e)
        }
    }
    
    private fun startForegroundNotification() {
        val notification = buildNotification()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground", e)
        }
    }
    
    private fun updateNotification() {
        try {
            notificationManager?.notify(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update notification", e)
        }
    }
    
    private fun buildNotification(): Notification {
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(this, 0, contentIntent, pendingFlags)
        
        val playIntent = Intent(this, MusicPlayerService::class.java).apply { action = ACTION_PLAY }
        val playPending = PendingIntent.getService(this, 1, playIntent, pendingFlags)
        
        val pauseIntent = Intent(this, MusicPlayerService::class.java).apply { action = ACTION_PAUSE }
        val pausePending = PendingIntent.getService(this, 2, pauseIntent, pendingFlags)
        
        val stopIntent = Intent(this, MusicPlayerService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 3, stopIntent, pendingFlags)
        
        val isPlaying = player?.isPlaying ?: false
        val title = currentVideoTitle ?: "YouTube Lite"
        val author = currentVideoAuthor ?: "Playing in background"
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(author)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentPending)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                if (isPlaying) pausePending else playPending
            )
            .addAction(R.drawable.ic_settings, "Stop", stopPending)
            .build()
    }
    
    override fun onDestroy() {
        player?.release()
        
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        
        instance = null
        super.onDestroy()
    }
}

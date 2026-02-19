package com.sensei.youtube.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.sensei.youtube.R
import com.sensei.youtube.ui.MainActivity

class NotificationService : Service() {
    
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaSession: MediaSessionCompat? = null
    private val binder = LocalBinder()
    
    companion object {
        const val CHANNEL_ID = "youtube_lite_playback"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_PLAY = "com.sensei.youtube.ACTION_PLAY"
        const val ACTION_PAUSE = "com.sensei.youtube.ACTION_PAUSE"
        const val ACTION_STOP = "com.sensei.youtube.ACTION_STOP"
        
        var currentVideoTitle: String? = null
        var currentVideoAuthor: String? = null
        private var isPlaying = false
        
        fun updatePlaybackState(context: Context, playing: Boolean) {
            isPlaying = playing
            val intent = Intent(context, NotificationService::class.java).apply {
                action = "UPDATE_NOTIFICATION"
            }
            context.startService(intent)
        }
        
        fun updateVideoInfo(context: Context, title: String, author: String) {
            currentVideoTitle = title
            currentVideoAuthor = author
            val intent = Intent(context, NotificationService::class.java).apply {
                action = "UPDATE_NOTIFICATION"
            }
            context.startService(intent)
        }
    }
    
    inner class LocalBinder : Binder() {
        fun getService(): NotificationService = this@NotificationService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "YouTubeLite::BackgroundPlayback"
        ).apply {
            acquire(10 * 60 * 1000L)
        }
        
        mediaSession = MediaSessionCompat(this, "YouTubeLiteMediaSession")
        mediaSession?.isActive = true
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                isPlaying = true
                updateNotification()
            }
            ACTION_PAUSE -> {
                isPlaying = false
                updateNotification()
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            "UPDATE_NOTIFICATION" -> {
                updateNotification()
            }
            else -> {
                startForegroundWithNotification()
            }
        }
        
        return START_STICKY
    }
    
    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }
    
    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }
    
    private fun buildNotification(): Notification {
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent, pendingIntentFlags
        )
        
        val playIntent = Intent(this, NotificationService::class.java).apply {
            action = ACTION_PLAY
        }
        val playPendingIntent = PendingIntent.getService(
            this, 1, playIntent, pendingIntentFlags
        )
        
        val pauseIntent = Intent(this, NotificationService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 2, pauseIntent, pendingIntentFlags
        )
        
        val stopIntent = Intent(this, NotificationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 3, stopIntent, pendingIntentFlags
        )
        
        val title = currentVideoTitle ?: "YouTube Lite"
        val author = currentVideoAuthor ?: "Background Playback"
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(author)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                if (isPlaying) pausePendingIntent else playPendingIntent
            )
            .addAction(
                R.drawable.ic_settings,
                "Stop",
                stopPendingIntent
            )
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .build()
    }
    
    override fun onDestroy() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        mediaSession?.release()
        super.onDestroy()
    }
}

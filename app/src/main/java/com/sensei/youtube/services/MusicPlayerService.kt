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
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import com.sensei.youtube.R
import com.sensei.youtube.ui.MainActivity

class MusicPlayerService : Service() {
    
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var playerNotificationManager: PlayerNotificationManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    companion object {
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
        
        fun isPlaying(): Boolean {
            return instance?.player?.isPlaying ?: false
        }
        
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
                e.printStackTrace()
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
        
        mediaSession = MediaSession.Builder(this, player!!).build()
        
        setupNotificationManager()
    }
    
    private fun setupNotificationManager() {
        playerNotificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            CHANNEL_ID
        ).apply {
            setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return currentVideoTitle ?: "YouTube Lite"
                }
                
                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    val intent = Intent(this@MusicPlayerService, MainActivity::class.java)
                    return PendingIntent.getActivity(
                        this@MusicPlayerService,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
                
                override fun getCurrentContentText(player: Player): CharSequence? {
                    return currentVideoAuthor ?: "Playing"
                }
                
                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): Bitmap? {
                    return null
                }
            })
            
            setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    if (ongoing) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                startForeground(
                                    notificationId,
                                    notification,
                                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                                )
                            } else {
                                startForeground(notificationId, notification)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                
                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopSelf()
                }
            })
        }.build()
        
        playerNotificationManager?.setPlayer(player)
        playerNotificationManager?.setMediaSessionToken(mediaSession?.sessionCompatToken!!)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                player?.play()
            }
            ACTION_PAUSE -> {
                player?.pause()
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
                
                wakeLock?.acquire(30 * 60 * 1000L)
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
            e.printStackTrace()
        }
    }
    
    override fun onDestroy() {
        playerNotificationManager?.setPlayer(null)
        player?.release()
        mediaSession?.release()
        
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        
        instance = null
        super.onDestroy()
    }
}

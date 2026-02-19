package com.sensei.youtube.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sensei.youtube.R
import com.sensei.youtube.ui.MainActivity

class AudioService : Service() {
    
    private var wakeLock: PowerManager.WakeLock? = null
    
    companion object {
        private const val TAG = "AudioService"
        const val CHANNEL_ID = "yt_playback"
        const val NOTIF_ID = 1001
        
        const val ACTION_PLAY = "play"
        const val ACTION_PAUSE = "pause"
        const val ACTION_STOP = "stop"
        const val ACTION_START = "start"
        
        var title: String = "YouTube Lite"
        var author: String = ""
        var isPlaying: Boolean = false
        var webViewCallback: ((String) -> Unit)? = null
        
        private var inst: AudioService? = null
        
        fun start(ctx: Context) {
            val i = Intent(ctx, AudioService::class.java).apply { action = ACTION_START }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(i)
                } else {
                    ctx.startService(i)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Start failed", e)
            }
        }
        
        fun stop(ctx: Context) {
            isPlaying = false
            try { ctx.stopService(Intent(ctx, AudioService::class.java)) } catch (_: Exception) {}
        }
        
        fun setPlaying(ctx: Context, playing: Boolean, t: String = "", a: String = "") {
            isPlaying = playing
            if (t.isNotEmpty()) title = t
            if (a.isNotEmpty()) author = a
            
            val i = Intent(ctx, AudioService::class.java).apply {
                action = if (playing) ACTION_PLAY else ACTION_PAUSE
            }
            try { ctx.startService(i) } catch (_: Exception) {}
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        inst = this
        Log.d(TAG, "Service created")
        
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "yt::audio")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_PLAY -> {
                isPlaying = true
                wakeLock?.acquire(30 * 60 * 1000L)
                webViewCallback?.invoke("play")
                updateNotif()
            }
            ACTION_PAUSE -> {
                isPlaying = false
                webViewCallback?.invoke("pause")
                updateNotif()
            }
            ACTION_STOP -> {
                isPlaying = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START, null -> {
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
            Log.e(TAG, "Foreground failed", e)
        }
    }
    
    private fun updateNotif() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID, buildNotif())
        } catch (_: Exception) {}
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
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(author.ifEmpty { "Background Playback" })
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pi)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                if (isPlaying) pausePi else playPi
            )
            .addAction(R.drawable.ic_close, "Stop", stopPi)
            .build()
    }
    
    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        wakeLock?.let { if (it.isHeld) it.release() }
        inst = null
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}

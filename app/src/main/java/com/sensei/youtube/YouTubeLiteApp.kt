package com.sensei.youtube

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.sensei.youtube.services.MusicPlayerService
import com.sensei.youtube.utils.AdBlocker
import com.sensei.youtube.utils.PreferenceManager

class YouTubeLiteApp : Application() {
    
    companion object {
        lateinit var instance: YouTubeLiteApp
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        PreferenceManager.init(this)
        AdBlocker.init(this)
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MusicPlayerService.CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

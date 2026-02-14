package com.jubair.youtube.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.jubair.youtube.MainActivity;
import com.jubair.youtube.R;

public class BackgroundAudioService extends Service {
    private static final String CHANNEL_ID = "SenseiMediaChannel";
    private MediaSessionCompat mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // মিডিয়া সেশন সেটআপ (Play/Pause বাটন এর জন্য)
        mediaSession = new MediaSessionCompat(this, "SenseiAudioService");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                // মেইন অ্যাক্টিভিটিতে সিগনাল পাঠাবে প্লে করার জন্য
                sendBroadcast(new Intent("ACTION_PLAY"));
                updateNotification(true);
            }

            @Override
            public void onPause() {
                sendBroadcast(new Intent("ACTION_PAUSE"));
                updateNotification(false);
            }
        });

        mediaSession.setActive(true);
        updateNotification(true);
    }

    private void updateNotification(boolean isPlaying) {
        int notificationAction = android.R.drawable.ic_media_pause;
        PendingIntent playPauseAction = null;

        Intent playIntent = new Intent(this, BackgroundAudioService.class); // মিডিয়া বাটন হ্যান্ডলার দরকার
        // সিম্পল রাখার জন্য আমরা শুধু নোটিফিকেশন শো করছি, বাটন লজিক JS দিয়ে হ্যান্ডেল হবে
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GoodTube Pro")
                .setContentText(isPlaying ? "Playing in Background" : "Paused")
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(isPlaying) // পজ থাকলে নোটিফিকেশন সরানো যাবে
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0)) // প্লে বাটন দেখাবে
                .addAction(new NotificationCompat.Action(
                        isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        "Pause", 
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this, 
                                isPlaying ? PlaybackStateCompat.ACTION_PAUSE : PlaybackStateCompat.ACTION_PLAY)))
                .build();

        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mediaSession.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Background Media Play",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}

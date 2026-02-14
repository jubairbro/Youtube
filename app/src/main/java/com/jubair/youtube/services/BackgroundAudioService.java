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
    private MediaSessionCompat mediaSession;
    private static final String CHANNEL_ID = "YouTubeLiteAudioChannel";
    private static final int NOTIFICATION_ID = 111;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();

        mediaSession = new MediaSessionCompat(this, "YouTubeLiteSession");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                sendBroadcast(new Intent("ACTION_PLAY"));
                updateNotification(true);
            }
            @Override
            public void onPause() {
                sendBroadcast(new Intent("ACTION_PAUSE"));
                updateNotification(false);
            }
            @Override
            public void onSkipToNext() {
                sendBroadcast(new Intent("ACTION_NEXT"));
            }
            @Override
            public void onSkipToPrevious() {
                sendBroadcast(new Intent("ACTION_PREV"));
            }
        });
        
        mediaSession.setActive(true);
    }

    public void updateNotification(boolean isPlaying) {
        int playPauseIcon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        String statusText = isPlaying ? "Playing" : "Paused";
        
        // Playback State সেট করা (লক স্ক্রিনের জন্য মাস্ট)
        int state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1)
                .build());

        // নোটিফিকেশন তৈরি
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("YouTube Lite")
                .setContentText(statusText)
                .setSubText("Background Play")
                .setLargeIcon(null) // চাইলে এখানে অ্যালবাম আর্ট দেওয়া যায়
                .setOngoing(isPlaying) // প্লে থাকলে নোটিফিকেশন সরবে না
                .setShowWhen(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE))
                // মিডিয়া স্টাইল (সবচেয়ে গুরুত্বপূর্ণ)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2)) // Prev, Play/Pause, Next
                // অ্যাকশন বাটন
                .addAction(android.R.drawable.ic_media_previous, "Prev", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
                .addAction(playPauseIcon, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(this, isPlaying ? PlaybackStateCompat.ACTION_PAUSE : PlaybackStateCompat.ACTION_PLAY))
                .addAction(android.R.drawable.ic_media_next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        
        if (intent != null && intent.hasExtra("IS_PLAYING")) {
            boolean isPlaying = intent.getBooleanExtra("IS_PLAYING", false);
            updateNotification(isPlaying);
        } else {
            // সার্ভিস যেন কিল না হয় তার জন্য ডিফল্ট নোটিফিকেশন
            updateNotification(false); 
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mediaSession.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            // চ্যানেল ইম্পর্টেন্স 'LOW' দেওয়া হয়েছে যাতে সাউন্ড না করে শুধু ভিজুয়্যাল থাকে
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Background Playback", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows media controls for YouTube Lite");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(channel);
        }
    }
}

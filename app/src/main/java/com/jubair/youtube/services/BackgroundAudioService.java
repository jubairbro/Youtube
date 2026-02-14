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
    private static final String CHANNEL_ID = "YouTubeLiteAudio";

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();

        mediaSession = new MediaSessionCompat(this, "YouTubeLite");
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
        int icon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        String title = isPlaying ? "Playing" : "Paused";
        
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED, 0, 1)
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .build());

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("YouTube Lite")
                .setContentText(title)
                .setOngoing(isPlaying)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .addAction(android.R.drawable.ic_media_previous, "Prev", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
                .addAction(icon, "Play/Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(this, isPlaying ? PlaybackStateCompat.ACTION_PAUSE : PlaybackStateCompat.ACTION_PLAY))
                .addAction(android.R.drawable.ic_media_next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
                .build();

        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        if (intent != null && intent.hasExtra("IS_PLAYING")) {
            updateNotification(intent.getBooleanExtra("IS_PLAYING", false));
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
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Audio Player", NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(channel);
        }
    }
}

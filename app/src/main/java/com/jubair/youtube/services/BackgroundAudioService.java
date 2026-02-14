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
import com.jubair.youtube.MainActivity;
import com.jubair.youtube.R;

public class BackgroundAudioService extends Service {
    
    public static boolean isServiceRunning = false;
    private MediaSessionCompat mediaSession;
    private static final String CHANNEL_ID = "YouTubeLiteAudioChannel";
    private static final int NOTIFICATION_ID = 111;

    public static final String ACTION_PLAY = "com.jubair.youtube.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.jubair.youtube.ACTION_PAUSE";
    public static final String ACTION_NEXT = "com.jubair.youtube.ACTION_NEXT";
    public static final String ACTION_PREV = "com.jubair.youtube.ACTION_PREV";

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true;
        createChannel();
        mediaSession = new MediaSessionCompat(this, "YouTubeLiteSession");
        mediaSession.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_PLAY.equals(action)) sendBroadcastToActivity("PLAY");
            else if (ACTION_PAUSE.equals(action)) sendBroadcastToActivity("PAUSE");
            else if (ACTION_NEXT.equals(action)) sendBroadcastToActivity("NEXT");
            else if (ACTION_PREV.equals(action)) sendBroadcastToActivity("PREV");

            if (intent.hasExtra("IS_PLAYING")) {
                updateNotification(intent.getBooleanExtra("IS_PLAYING", false));
            }
        }
        return START_STICKY;
    }

    private void sendBroadcastToActivity(String command) {
        Intent intent = new Intent("MEDIA_CONTROL");
        intent.putExtra("COMMAND", command);
        sendBroadcast(intent);
    }

    public void updateNotification(boolean isPlaying) {
        int playPauseIcon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                .setState(isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED, 0, 1)
                .build());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("YouTube Lite")
                .setContentText(isPlaying ? "Playing Content..." : "Music Paused")
                .setOngoing(isPlaying)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .addAction(android.R.drawable.ic_media_previous, "Prev", getPendingIntent(ACTION_PREV))
                .addAction(playPauseIcon, "Play", getPendingIntent(isPlaying ? ACTION_PAUSE : ACTION_PLAY))
                .addAction(android.R.drawable.ic_media_next, "Next", getPendingIntent(ACTION_NEXT));

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private PendingIntent getPendingIntent(String action) {
        Intent intent = new Intent(this, BackgroundAudioService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onDestroy() {
        isServiceRunning = false;
        mediaSession.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager m = getSystemService(NotificationManager.class);
            m.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "Playback", NotificationManager.IMPORTANCE_LOW));
        }
    }
}

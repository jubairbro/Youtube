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
import android.util.Base64;
import androidx.core.app.NotificationCompat;
import com.jubair.youtube.MainActivity;
import com.jubair.youtube.R;

public class BackgroundAudioService extends Service {
    
    public static boolean isServiceRunning = false;
    private MediaSessionCompat mediaSession;
    
    // Encoded Strings (Base64)
    private static final String CH_ID = new String(Base64.decode("WXVUb2JlTGl0ZUNoYW5uZWw=", Base64.DEFAULT)); 
    private static final String APP_NAME = new String(Base64.decode("WXVUb2JlIExpdGU=", Base64.DEFAULT)); 
    private static final String SENSEI = new String(Base64.decode("SnViYWlyIFNlbnNlaQ==", Base64.DEFAULT)); 

    public static final String ACTION_PLAY = "com.jubair.youtube.PLAY";
    public static final String ACTION_PAUSE = "com.jubair.youtube.PAUSE";
    public static final String ACTION_NEXT = "com.jubair.youtube.NEXT";
    public static final String ACTION_PREV = "com.jubair.youtube.PREV";

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true;
        createChannel();
        mediaSession = new MediaSessionCompat(this, SENSEI);
        mediaSession.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_PLAY.equals(action)) sendToActivity("PLAY");
            else if (ACTION_PAUSE.equals(action)) sendToActivity("PAUSE");
            else if (ACTION_NEXT.equals(action)) sendToActivity("NEXT");
            else if (ACTION_PREV.equals(action)) sendToActivity("PREV");

            if (intent.hasExtra("IS_PLAYING")) {
                updateNotification(intent.getBooleanExtra("IS_PLAYING", false));
            }
        }
        return START_STICKY;
    }

    private void sendToActivity(String cmd) {
        Intent i = new Intent("MEDIA_CONTROL");
        i.putExtra("COMMAND", cmd);
        sendBroadcast(i);
    }

    public void updateNotification(boolean isPlaying) {
        int icon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CH_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(APP_NAME)
                .setContentText(isPlaying ? "Active: " + SENSEI : "Paused")
                .setOngoing(isPlaying)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .addAction(android.R.drawable.ic_media_previous, "P", getPI(ACTION_PREV))
                .addAction(icon, "PP", getPI(isPlaying ? ACTION_PAUSE : ACTION_PLAY))
                .addAction(android.R.drawable.ic_media_next, "N", getPI(ACTION_NEXT));

        startForeground(111, builder.build());
    }

    private PendingIntent getPI(String action) {
        Intent i = new Intent(this, BackgroundAudioService.class);
        i.setAction(action);
        return PendingIntent.getService(this, 0, i, PendingIntent.FLAG_IMMUTABLE);
    }

    @Override public void onDestroy() { isServiceRunning = false; mediaSession.release(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager m = getSystemService(NotificationManager.class);
            m.createNotificationChannel(new NotificationChannel(CH_ID, "Playback", NotificationManager.IMPORTANCE_LOW));
        }
    }
}

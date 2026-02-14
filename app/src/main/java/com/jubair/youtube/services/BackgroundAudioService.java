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
    
    // এই ভেরিয়েবলটি ক্লাসের ভেতরে থাকতে হবে
    public static boolean isServiceRunning = false;
    
    private MediaSessionCompat mediaSession;
    private static final String CHANNEL_ID = "YouTubeLiteAudioChannel";
    private static final int NOTIFICATION_ID = 111;

    // অ্যাকশন কমান্ডস
    public static final String ACTION_PLAY = "com.jubair.youtube.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.jubair.youtube.ACTION_PAUSE";
    public static final String ACTION_NEXT = "com.jubair.youtube.ACTION_NEXT";
    public static final String ACTION_PREV = "com.jubair.youtube.ACTION_PREV";
    public static final String ACTION_STOP = "com.jubair.youtube.ACTION_STOP";

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true; // সার্ভিস চালু হলে সত্য হবে
        createChannel();

        // লক স্ক্রিন কন্ট্রোলের জন্য মিডিয়া সেশন
        mediaSession = new MediaSessionCompat(this, "YouTubeLiteSession");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            
            // ১. নোটিফিকেশন বাটন হ্যান্ডলিং
            if (ACTION_PLAY.equals(action)) {
                sendBroadcastToActivity("PLAY");
                updateNotification(true);
            } else if (ACTION_PAUSE.equals(action)) {
                sendBroadcastToActivity("PAUSE");
                updateNotification(false);
            } else if (ACTION_NEXT.equals(action)) {
                sendBroadcastToActivity("NEXT");
            } else if (ACTION_PREV.equals(action)) {
                sendBroadcastToActivity("PREV");
            } else if (ACTION_STOP.equals(action)) {
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }
            
            // ২. মেইন অ্যাক্টিভিটি থেকে স্ট্যাটাস আপডেট আসলে
            if (intent.hasExtra("IS_PLAYING")) {
                boolean isPlaying = intent.getBooleanExtra("IS_PLAYING", false);
                updateNotification(isPlaying);
            }
        }
        return START_STICKY;
    }

    // অ্যাক্টিভিটিতে সিগনাল পাঠানো
    private void sendBroadcastToActivity(String command) {
        Intent intent = new Intent("MEDIA_CONTROL");
        intent.putExtra("COMMAND", command);
        sendBroadcast(intent);
    }

    public void updateNotification(boolean isPlaying) {
        // প্লে/পজ আইকন এবং টেক্সট সেট করা
        int playPauseIcon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        String actionToTrigger = isPlaying ? ACTION_PAUSE : ACTION_PLAY;
        String statusText = isPlaying ? "Playing" : "Paused";

        // মিডিয়া সেশন স্টেট আপডেট (লক স্ক্রিনের জন্য)
        int state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1)
                .build());

        // নোটিফিকেশন বিল্ডার
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("YouTube Lite")
                .setContentText(statusText)
                .setSubText("Background Play")
                .setOngoing(isPlaying) // প্লে থাকলে সরানো যাবে না
                .setShowWhen(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
                
                // মিডিয়া স্টাইল (নোটিফিকেশন বড় দেখানোর জন্য)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));

        // বাটন ১: Prev
        builder.addAction(android.R.drawable.ic_media_previous, "Prev", 
                generateActionIntent(ACTION_PREV));

        // বাটন ২: Play/Pause (ডাইনামিক)
        builder.addAction(playPauseIcon, "Play", 
                generateActionIntent(actionToTrigger));

        // বাটন ৩: Next
        builder.addAction(android.R.drawable.ic_media_next, "Next", 
                generateActionIntent(ACTION_NEXT));
        
        // ক্লোজ বাটন (অপশনাল)
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", 
                generateActionIntent(ACTION_STOP));

        startForeground(NOTIFICATION_ID, builder.build());
    }

    // বাটন ক্লিকের জন্য পেন্ডিং ইনটেন্ট জেনারেটর
    private PendingIntent generateActionIntent(String action) {
        Intent intent = new Intent(this, BackgroundAudioService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onDestroy() {
        isServiceRunning = false; // সার্ভিস বন্ধ হলে মিথ্যা হবে
        if (mediaSession != null) {
            mediaSession.release();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Background Playback", NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(channel);
        }
    }
}

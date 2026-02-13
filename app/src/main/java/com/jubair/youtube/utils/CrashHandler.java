package com.jubair.youtube.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Looper;
import android.widget.ScrollView;
import android.widget.TextView;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private Context context;

    public CrashHandler(Context context) {
        this.context = context;
    }

    @Override
    public void uncaughtException(Thread t, final Throwable e) {
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                showErrorDialog(e);
                Looper.loop();
            }
        }.start();
        
        try {
            Thread.sleep(2000); // ডায়লগ দেখার জন্য সময় দেওয়া
        } catch (InterruptedException ex) {}
    }

    private void showErrorDialog(Throwable e) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("SYSTEM CRASH DETECTED 💀");
        builder.setCancelable(false);
        
        // এরর লগ তৈরি করা
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n\n");
        for (StackTraceElement el : e.getStackTrace()) {
            sb.append(el.toString()).append("\n");
        }

        ScrollView scrollView = new ScrollView(context);
        TextView textView = new TextView(context);
        textView.setText(sb.toString());
        textView.setTextColor(0xFFFF0000); // Red Color
        textView.setPadding(20, 20, 20, 20);
        scrollView.addView(textView);

        builder.setView(scrollView);
        builder.setPositiveButton("RESTART APP", (dialog, which) -> {
            System.exit(1);
        });
        builder.show();
    }
}

package com.jubair.youtube.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.jubair.youtube.managers.SharedPrefManager;

public class DialogManager {
    public static void showWelcomeDialog(final Context context) {
        if (!SharedPrefManager.getInstance(context).shouldShowDialog()) {
            return;
        }

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // --- 1. মেইন কন্টেইনার (বর্ডার সহ) ---
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(5, 5, 5, 5); // বর্ডারের পুরুত্ব
        mainLayout.setBackgroundColor(Color.parseColor("#00FF00")); // বর্ডার কালার (Lime)

        // --- 2. ইনার কন্টেইনার (ডার্ক ব্যাকগ্রাউন্ড) ---
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setBackgroundColor(Color.parseColor("#111111")); // Dark BG
        contentLayout.setPadding(50, 40, 50, 40);
        
        // --- 3. টাইটেল ---
        TextView title = new TextView(context);
        title.setText("Made by : JUBAIR SENSEI");
        title.setTextColor(Color.parseColor("#00FF00")); // Lime Text
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 30);
        contentLayout.addView(title);

        // --- 4. মেসেজ ---
        TextView msg = new TextView(context);
        msg.setText("Protocol Activated:\n\n• Native AdBlocker\n• Background Play\n• PiP Mode Enabled");
        msg.setTextColor(Color.parseColor("#DDDDDD")); // Off-White
        msg.setTextSize(14);
        msg.setLineSpacing(0, 1.2f);
        contentLayout.addView(msg);

        // --- 5. চেকবক্স (Don't Show Again) ---
        CheckBox checkBox = new CheckBox(context);
        checkBox.setText("Don't show this again");
        checkBox.setTextColor(Color.GRAY);
        checkBox.setPadding(10, 30, 0, 30);
        
        // চেকবক্সের কালার গ্রিন করা (API 21+)
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.GREEN));
        }
        contentLayout.addView(checkBox);

        // --- 6. বাটন লেআউট ---
        LinearLayout btnLayout = new LinearLayout(context);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(Gravity.CENTER);
        btnLayout.setPadding(0, 20, 0, 0);

        // টেলিগ্রাম বাটন
        Button btnTg = new Button(context);
        btnTg.setText("TELEGRAM");
        btnTg.setTextColor(Color.BLACK);
        btnTg.setTextSize(12);
        btnTg.setBackgroundColor(Color.parseColor("#00FF00")); // Lime BG
        LinearLayout.LayoutParams paramsTg = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        paramsTg.setMargins(0, 0, 10, 0);
        btnTg.setLayoutParams(paramsTg);
        
        btnTg.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Jubairsensei"));
            context.startActivity(intent);
            saveAndDismiss(dialog, checkBox, context);
        });

        // ওকে বাটন
        Button btnOk = new Button(context);
        btnOk.setText("ENTER");
        btnOk.setTextColor(Color.parseColor("#00FF00")); // Lime Text
        btnOk.setTextSize(12);
        btnOk.setBackgroundColor(Color.parseColor("#222222")); // Dark Grey BG
        LinearLayout.LayoutParams paramsOk = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        paramsOk.setMargins(10, 0, 0, 0);
        btnOk.setLayoutParams(paramsOk);

        btnOk.setOnClickListener(v -> saveAndDismiss(dialog, checkBox, context));

        btnLayout.addView(btnTg);
        btnLayout.addView(btnOk);
        contentLayout.addView(btnLayout);

        // কন্টেন্ট মেইনে এড করা
        mainLayout.addView(contentLayout);

        dialog.setContentView(mainLayout);
        
        if (dialog.getWindow() != null) {
            // ডায়লগের ডিফল্ট ব্যাকগ্রাউন্ড ট্রান্সপারেন্ট করা যাতে আমাদের কাস্টম বর্ডার দেখা যায়
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(Gravity.CENTER);
        }
        
        dialog.setCancelable(false);
        dialog.show();
    }

    private static void saveAndDismiss(Dialog dialog, CheckBox checkBox, Context context) {
        if (checkBox.isChecked()) {
            SharedPrefManager.getInstance(context).setDialogHidden(true);
        }
        dialog.dismiss();
    }
}

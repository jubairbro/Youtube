package com.jubair.youtube.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
        // চেক করা হচ্ছে ইউজার আগে 'Don't show again' দিয়েছে কিনা
        if (!SharedPrefManager.getInstance(context).shouldShowDialog()) {
            return;
        }

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // মেইন কন্টেইনার (ক্লিন ডার্ক গ্রে)
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#222222"));
        layout.setPadding(60, 50, 60, 50);
        
        // টাইটেল (আপনার পছন্দ মতো টেক্সট দিন)
        TextView title = new TextView(context);
        title.setText("Welcome Jubair Sensei");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 30);
        layout.addView(title);

        // মেসেজ বডি (আপনার পছন্দ মতো টেক্সট দিন)
        TextView msg = new TextView(context);
        msg.setText("This is your personal YouTube Pro app.\n\nFeatures Active:\n- AdBlocker\n- Background Play\n- PiP Mode");
        msg.setTextColor(Color.LTGRAY);
        msg.setTextSize(16);
        msg.setLineSpacing(0, 1.2f);
        layout.addView(msg);

        // চেকবক্স
        CheckBox checkBox = new CheckBox(context);
        checkBox.setText("Don't show again");
        checkBox.setTextColor(Color.GRAY);
        checkBox.setPadding(0, 40, 0, 30);
        layout.addView(checkBox);

        // ওকে বাটন
        Button btnOk = new Button(context);
        btnOk.setText("OK, Let's Go");
        btnOk.setTextColor(Color.WHITE);
        btnOk.setBackgroundColor(Color.parseColor("#CC0000")); // ইউটিউব রেড কালার
        btnOk.setOnClickListener(v -> {
            if (checkBox.isChecked()) {
                SharedPrefManager.getInstance(context).setDialogHidden(true);
            }
            dialog.dismiss();
        });
        layout.addView(btnOk);

        dialog.setContentView(layout);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(Gravity.CENTER);
        }
        
        dialog.setCancelable(false);
        dialog.show();
    }
}

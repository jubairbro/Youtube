package com.jubair.youtube.managers;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {
    private static final String PREF_NAME = "SenseiPrefs";
    private static final String KEY_SHOW_DIALOG = "show_welcome_dialog";
    private static SharedPrefManager instance;
    private final SharedPreferences sharedPreferences;

    private SharedPrefManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }

    public boolean shouldShowDialog() {
        return sharedPreferences.getBoolean(KEY_SHOW_DIALOG, true);
    }

    public void setDialogHidden(boolean hidden) {
        sharedPreferences.edit().putBoolean(KEY_SHOW_DIALOG, !hidden).apply();
    }
}

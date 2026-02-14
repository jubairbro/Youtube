package com.jubair.youtube;

import android.app.Application;

public class SenseiApp extends Application {
    private static SenseiApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static SenseiApp getInstance() {
        return instance;
    }
}

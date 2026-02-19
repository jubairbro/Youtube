package com.sensei.youtube.utils

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {
    
    private const val PREF_NAME = "youtube_lite_prefs"
    private const val KEY_FIRST_RUN = "is_first_run"
    private const val KEY_TG_JOINED = "tg_joined"
    private const val KEY_BACKGROUND_PLAY = "background_play"
    private const val KEY_AD_BLOCK = "ad_block"
    private const val KEY_DESKTOP_MODE = "desktop_mode"
    
    private lateinit var prefs: SharedPreferences
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    var isFirstRun: Boolean
        get() = prefs.getBoolean(KEY_FIRST_RUN, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_RUN, value).apply()
    
    var isTgJoined: Boolean
        get() = prefs.getBoolean(KEY_TG_JOINED, false)
        set(value) = prefs.edit().putBoolean(KEY_TG_JOINED, value).apply()
    
    var isBackgroundPlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACKGROUND_PLAY, true)
        set(value) = prefs.edit().putBoolean(KEY_BACKGROUND_PLAY, value).apply()
    
    var isAdBlockEnabled: Boolean
        get() = prefs.getBoolean(KEY_AD_BLOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_AD_BLOCK, value).apply()
    
    var isDesktopMode: Boolean
        get() = prefs.getBoolean(KEY_DESKTOP_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DESKTOP_MODE, value).apply()
}

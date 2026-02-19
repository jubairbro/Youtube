package com.sensei.youtube.utils

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {
    
    private const val PREF_NAME = "youtube_lite"
    
    private lateinit var prefs: SharedPreferences
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    var isFirstRun: Boolean
        get() = prefs.getBoolean("first_run", true)
        set(value) = prefs.edit().putBoolean("first_run", value).apply()
    
    var isTgJoined: Boolean
        get() = prefs.getBoolean("tg_joined", false)
        set(value) = prefs.edit().putBoolean("tg_joined", value).apply()
    
    var isBackgroundPlayEnabled: Boolean
        get() = prefs.getBoolean("bg_play", true)
        set(value) = prefs.edit().putBoolean("bg_play", value).apply()
    
    var isAdBlockEnabled: Boolean
        get() = prefs.getBoolean("ad_block", true)
        set(value) = prefs.edit().putBoolean("ad_block", value).apply()
    
    var isDesktopMode: Boolean
        get() = prefs.getBoolean("desktop", false)
        set(value) = prefs.edit().putBoolean("desktop", value).apply()
}

package com.sensei.youtube.util

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    
    private lateinit var prefs: SharedPreferences
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences("yt_lite", Context.MODE_PRIVATE)
    }
    
    var firstRun: Boolean
        get() = prefs.getBoolean("first", true)
        set(v) = prefs.edit().putBoolean("first", v).apply()
    
    var bgPlay: Boolean
        get() = prefs.getBoolean("bg", true)
        set(v) = prefs.edit().putBoolean("bg", v).apply()
    
    var adBlock: Boolean
        get() = prefs.getBoolean("ads", true)
        set(v) = prefs.edit().putBoolean("ads", v).apply()
    
    var desktop: Boolean
        get() = prefs.getBoolean("desk", false)
        set(v) = prefs.edit().putBoolean("desk", v).apply()
    
    var tgShown: Boolean
        get() = prefs.getBoolean("tg", false)
        set(v) = prefs.edit().putBoolean("tg", v).apply()
}

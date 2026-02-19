package com.sensei.youtube

import android.app.Application
import com.sensei.youtube.utils.AdBlocker
import com.sensei.youtube.utils.PreferenceManager

class YouTubeLiteApp : Application() {
    
    companion object {
        lateinit var instance: YouTubeLiteApp
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        PreferenceManager.init(this)
        AdBlocker.init(this)
    }
}

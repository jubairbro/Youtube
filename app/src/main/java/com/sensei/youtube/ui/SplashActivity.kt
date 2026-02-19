package com.sensei.youtube.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.sensei.youtube.databinding.ActivitySplashBinding
import com.sensei.youtube.util.Prefs

class SplashActivity : AppCompatActivity() {
    
    private lateinit var bind: ActivitySplashBinding
    
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        bind = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(bind.root)
        
        Handler(Looper.getMainLooper()).postDelayed({
            val i = if (Prefs.firstRun) Intent(this, IntroActivity::class.java)
                   else Intent(this, MainActivity::class.java)
            startActivity(i)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 1000)
    }
}

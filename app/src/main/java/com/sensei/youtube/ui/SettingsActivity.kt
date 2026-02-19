package com.sensei.youtube.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sensei.youtube.databinding.ActivitySettingsBinding
import com.sensei.youtube.util.Prefs

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var bind: ActivitySettingsBinding
    
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        bind = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(bind.root)
        
        bind.toolbar.setNavigationOnClickListener { finish() }
        
        bind.swBg.isChecked = Prefs.bgPlay
        bind.swAds.isChecked = Prefs.adBlock
        bind.swDesk.isChecked = Prefs.desktop
        
        bind.swBg.setOnCheckedChangeListener { _, v -> Prefs.bgPlay = v }
        bind.swAds.setOnCheckedChangeListener { _, v -> Prefs.adBlock = v }
        bind.swDesk.setOnCheckedChangeListener { _, v -> Prefs.desktop = v }
        
        bind.btnReload.setOnClickListener {
            Toast.makeText(this, "Restart app to apply changes", Toast.LENGTH_SHORT).show()
        }
        
        bind.btnClear.setOnClickListener {
            cacheDir.deleteRecursively()
            Toast.makeText(this, "Cache cleared. Restart app.", Toast.LENGTH_SHORT).show()
        }
        
        try {
            val pi = packageManager.getPackageInfo(packageName, 0)
            bind.version.text = "Version ${pi.versionName}"
        } catch (e: Exception) {
            bind.version.text = "Version 2.0.0"
        }
    }
}

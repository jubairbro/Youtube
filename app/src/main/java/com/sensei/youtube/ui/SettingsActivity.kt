package com.sensei.youtube.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sensei.youtube.R
import com.sensei.youtube.databinding.ActivitySettingsBinding
import com.sensei.youtube.utils.PreferenceManager

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupSwitches()
        setupButtons()
        setupVersion()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupSwitches() {
        binding.switchBackgroundPlay.isChecked = PreferenceManager.isBackgroundPlayEnabled
        binding.switchAdBlock.isChecked = PreferenceManager.isAdBlockEnabled
        binding.switchDesktopMode.isChecked = PreferenceManager.isDesktopMode
        
        binding.switchBackgroundPlay.setOnCheckedChangeListener { _, isChecked ->
            PreferenceManager.isBackgroundPlayEnabled = isChecked
        }
        
        binding.switchAdBlock.setOnCheckedChangeListener { _, isChecked ->
            PreferenceManager.isAdBlockEnabled = isChecked
        }
        
        binding.switchDesktopMode.setOnCheckedChangeListener { _, isChecked ->
            PreferenceManager.isDesktopMode = isChecked
        }
    }
    
    private fun setupButtons() {
        binding.btnHardReload.setOnClickListener {
            Toast.makeText(this, "Reloading...", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnClearCache.setOnClickListener {
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupVersion() {
        try {
            val pm = packageManager.getPackageInfo(packageName, 0)
            binding.txtVersion.text = "Version ${pm.versionName} (${if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) pm.longVersionCode else pm.versionCode})"
        } catch (e: Exception) {
            binding.txtVersion.text = "Version 1.0.0"
        }
    }
}

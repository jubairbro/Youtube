package com.sensei.youtube.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        binding.toolbar.setNavigationOnClickListener { finish() }
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
            Toast.makeText(this, "Settings saved. Restart app to apply.", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnClearCache.setOnClickListener {
            cacheDir.deleteRecursively()
            Toast.makeText(this, "Cache cleared. Restart app.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupVersion() {
        try {
            val pi = packageManager.getPackageInfo(packageName, 0)
            binding.txtVersion.text = "Version ${pi.versionName}"
        } catch (e: Exception) {
            binding.txtVersion.text = "Version 1.1.0"
        }
    }
}

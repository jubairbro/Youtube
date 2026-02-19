package com.sensei.youtube.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sensei.youtube.BuildConfig
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
            (getMainActivity())?.toggleDesktopMode(isChecked)
        }
    }
    
    private fun setupButtons() {
        binding.btnHardReload.setOnClickListener {
            (getMainActivity())?.hardReload()
            Toast.makeText(this, "Reloading...", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnClearCache.setOnClickListener {
            (getMainActivity())?.clearAllCache()
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupVersion() {
        binding.txtVersion.text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }
    
    private fun getMainActivity(): MainActivity? {
        return try {
            (application as? com.sensei.youtube.YouTubeLiteApp)
                ?.let { null }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun openTelegram() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=JubairSensei"))
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/JubairSensei"))
            startActivity(intent)
        }
    }
}

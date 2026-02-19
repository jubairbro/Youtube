package com.sensei.youtube.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.sensei.youtube.R
import com.sensei.youtube.databinding.ActivityIntroBinding
import com.sensei.youtube.utils.PreferenceManager

class IntroActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityIntroBinding
    private val dots = mutableListOf<View>()
    
    private val slides = listOf(
        Slide(R.drawable.intro_bg_1, R.string.intro_title_1, R.string.intro_desc_1),
        Slide(R.drawable.intro_bg_2, R.string.intro_title_2, R.string.intro_desc_2),
        Slide(R.drawable.intro_bg_3, R.string.intro_title_3, R.string.intro_desc_3)
    )
    
    data class Slide(val iconRes: Int, val titleRes: Int, val descRes: Int)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViewPager()
        setupDots()
        setupButtons()
    }
    
    private fun setupViewPager() {
        binding.viewPager.adapter = IntroAdapter()
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                updateButton(position)
            }
        })
    }
    
    private fun setupDots() {
        dots.clear()
        binding.dotsIndicator.removeAllViews()
        
        repeat(slides.size) { i ->
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(if (i == 0) 24.dp else 8.dp, 8.dp).apply {
                    marginEnd = 4.dp
                }
                setBackgroundResource(if (i == 0) R.drawable.bg_button_primary else R.drawable.bg_button_secondary)
            }
            dots.add(dot)
            binding.dotsIndicator.addView(dot)
        }
    }
    
    private fun updateDots(position: Int) {
        dots.forEachIndexed { index, dot ->
            dot.layoutParams = LinearLayout.LayoutParams(if (index == position) 24.dp else 8.dp, 8.dp).apply {
                marginEnd = 4.dp
            }
            dot.setBackgroundResource(if (index == position) R.drawable.bg_button_primary else R.drawable.bg_button_secondary)
        }
    }
    
    private fun updateButton(position: Int) {
        binding.btnNext.text = if (position == slides.size - 1) getString(R.string.get_started) else getString(R.string.next)
    }
    
    private fun setupButtons() {
        binding.btnSkip.setOnClickListener { finishIntro() }
        binding.btnNext.setOnClickListener {
            if (binding.viewPager.currentItem == slides.size - 1) {
                finishIntro()
            } else {
                binding.viewPager.currentItem += 1
            }
        }
    }
    
    private fun finishIntro() {
        PreferenceManager.isFirstRun = false
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
    
    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
    
    private inner class IntroAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<IntroViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = IntroViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_intro_slide, parent, false)
        )
        override fun onBindViewHolder(holder: IntroViewHolder, position: Int) = holder.bind(slides[position])
        override fun getItemCount() = slides.size
    }
    
    private inner class IntroViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        fun bind(slide: Slide) {
            itemView.findViewById<ImageView>(R.id.img_icon).setImageResource(slide.iconRes)
            itemView.findViewById<TextView>(R.id.txt_title).setText(slide.titleRes)
            itemView.findViewById<TextView>(R.id.txt_description).setText(slide.descRes)
        }
    }
}

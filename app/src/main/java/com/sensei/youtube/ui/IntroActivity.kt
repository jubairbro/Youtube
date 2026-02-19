package com.sensei.youtube.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.sensei.youtube.R
import com.sensei.youtube.databinding.ActivityIntroBinding
import com.sensei.youtube.util.Prefs

class IntroActivity : AppCompatActivity() {
    
    private lateinit var bind: ActivityIntroBinding
    private val dots = mutableListOf<View>()
    
    private val slides = listOf(
        Slide(R.drawable.ic_bg_play, "Background Play", "Continue playing audio when screen is off or app is minimized"),
        Slide(R.drawable.ic_bg_safe, "Secure Login", "Your Google account stays secure with encrypted cookies"),
        Slide(R.drawable.ic_bg_data, "Save Data", "Block ads and save bandwidth with smart ad blocking")
    )
    
    data class Slide(val icon: Int, val title: String, val desc: String)
    
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        bind = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(bind.root)
        
        bind.pager.adapter = Adapter()
        bind.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(p: Int) {
                updateDots(p)
                bind.next.text = if (p == slides.lastIndex) "Get Started" else "Next"
            }
        })
        
        setupDots()
        
        bind.skip.setOnClickListener { done() }
        bind.next.setOnClickListener {
            if (bind.pager.currentItem == slides.lastIndex) done()
            else bind.pager.currentItem += 1
        }
    }
    
    private fun setupDots() {
        repeat(slides.size) { i ->
            val v = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(if (i == 0) 48 else 16, 16).apply { marginEnd = 8 }
                setBackgroundResource(if (i == 0) R.drawable.bg_dot_on else R.drawable.bg_dot_off)
            }
            dots.add(v)
            bind.dots.addView(v)
        }
    }
    
    private fun updateDots(p: Int) {
        dots.forEachIndexed { i, v ->
            v.layoutParams = LinearLayout.LayoutParams(if (i == p) 48 else 16, 16).apply { marginEnd = 8 }
            v.setBackgroundResource(if (i == p) R.drawable.bg_dot_on else R.drawable.bg_dot_off)
        }
    }
    
    private fun done() {
        Prefs.firstRun = false
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
    
    private inner class Adapter : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_intro, p, false))
        override fun onBindViewHolder(h: VH, p: Int) { h.bind(slides[p]) }
        override fun getItemCount() = slides.size
    }
    
    private inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        fun bind(s: Slide) {
            itemView.findViewById<ImageView>(R.id.icon).setImageResource(s.icon)
            itemView.findViewById<TextView>(R.id.title).text = s.title
            itemView.findViewById<TextView>(R.id.desc).text = s.desc
        }
    }
}

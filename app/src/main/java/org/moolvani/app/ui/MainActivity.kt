package org.moolvani.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.moolvani.app.MoolVaniApplication
import org.moolvani.app.R
import org.moolvani.app.databinding.ActivityMainBinding
import org.moolvani.app.ui.courses.CoursesFragment
import org.moolvani.app.ui.interactive.InteractiveFragment
import org.moolvani.app.ui.phrases.PhrasesFragment
import org.moolvani.app.ui.translator.TranslatorFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentTab: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        checkAudioPermission()

        if (savedInstanceState == null) {
            selectTab(0)
        }
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                101
            )
        }
    }

    private fun setupNavigation() {
        binding.navTranslator.setOnClickListener { selectTab(0) }
        binding.navPhrases.setOnClickListener { selectTab(1) }
        binding.navCourses.setOnClickListener { selectTab(2) }
        binding.navInteractive.setOnClickListener { selectTab(3) }
    }

    private fun selectTab(tabIndex: Int) {
        currentTab = tabIndex

        val navButtons = listOf(
            Triple(binding.navTranslator, binding.badgeRt, binding.labelRt),
            Triple(binding.navPhrases, binding.badgeCp, binding.labelCp),
            Triple(binding.navCourses, binding.badgeC, binding.labelC),
            Triple(binding.navInteractive, binding.badgeFc, binding.labelFc)
        )

        for ((idx, item) in navButtons.withIndex()) {
            val (container, badge, label) = item
            if (idx == tabIndex) {
                container.setBackgroundResource(R.drawable.bg_nav_selected)
                badge.setBackgroundResource(R.drawable.bg_nav_badge_selected)
                badge.setTextColor(resources.getColor(R.color.navy_primary, null))
                label.setTextColor(resources.getColor(R.color.white, null))
                label.setTypeface(null, Typeface.BOLD)
            } else {
                container.setBackgroundColor(resources.getColor(R.color.transparent, null))
                badge.setBackgroundResource(R.drawable.bg_nav_badge_unselected)
                badge.setTextColor(0xFF94A3B8.toInt())
                label.setTextColor(0xFF94A3B8.toInt())
                label.setTypeface(null, Typeface.NORMAL)
            }
        }

        val fragment: Fragment = when (tabIndex) {
            0 -> TranslatorFragment()
            1 -> PhrasesFragment()
            2 -> CoursesFragment()
            3 -> InteractiveFragment()
            else -> TranslatorFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment)
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        MoolVaniApplication.instance.audioEngine.release()
        MoolVaniApplication.instance.speechHelper.destroy()
    }
}

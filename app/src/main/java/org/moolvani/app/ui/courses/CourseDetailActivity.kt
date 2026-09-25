package org.moolvani.app.ui.courses

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.moolvani.app.MoolVaniApplication
import org.moolvani.app.R
import org.moolvani.app.data.model.CourseItem
import org.moolvani.app.databinding.ActivityCourseDetailBinding
import org.moolvani.app.databinding.ItemCourseVocabularyBinding

class CourseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCourseDetailBinding
    private var courseCode: String = "NUMBERS"
    private var items: List<CourseItem> = emptyList()
    private var currentIndex: Int = 0
    private var isPlayingSequence: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCourseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        courseCode = intent.getStringExtra("EXTRA_COURSE_CODE") ?: "NUMBERS"
        val title = intent.getStringExtra("EXTRA_COURSE_TITLE") ?: "$courseCode (Course)"
        binding.tvDetailTitle.text = title

        binding.btnBackCourse.setOnClickListener { finish() }

        setupSpeedBar()
        setupPlayPauseButton()
        loadCourseItems()
    }

    private fun setupSpeedBar() {
        val app = MoolVaniApplication.instance
        val speedButtons = listOf(
            binding.btnSpeed075 to 0.75f,
            binding.btnSpeed100 to 1.0f,
            binding.btnSpeed125 to 1.25f,
            binding.btnSpeed150 to 1.5f
        )

        fun updateSpeedUi(activeSpeed: Float) {
            app.audioEngine.playbackSpeed = activeSpeed
            for ((btn, speed) in speedButtons) {
                if (speed == activeSpeed) {
                    btn.setBackgroundColor(resources.getColor(R.color.navy_primary, null))
                    btn.setTextColor(resources.getColor(R.color.white, null))
                } else {
                    btn.setBackgroundColor(resources.getColor(R.color.white, null))
                    btn.setTextColor(resources.getColor(R.color.navy_primary, null))
                }
            }
            Toast.makeText(this, "Audio Speed set to ${activeSpeed}x", Toast.LENGTH_SHORT).show()
        }

        for ((btn, speed) in speedButtons) {
            btn.setOnClickListener { updateSpeedUi(speed) }
        }
    }

    private fun setupPlayPauseButton() {
        binding.btnPlayPause.setOnClickListener {
            if (isPlayingSequence) {
                pauseSequence()
            } else {
                playSequence()
            }
        }
    }

    private fun playSequence() {
        if (items.isEmpty()) return
        isPlayingSequence = true
        binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
        playItemAt(currentIndex)
    }

    private fun pauseSequence() {
        isPlayingSequence = false
        val app = MoolVaniApplication.instance
        app.audioEngine.stop()
        binding.btnPlayPause.setImageResource(R.drawable.ic_play_arrow)
    }

    private fun playItemAt(index: Int) {
        if (!isPlayingSequence || index >= items.size) {
            pauseSequence()
            currentIndex = 0
            return
        }

        val item = items[index]
        currentIndex = index
        displayActiveItem(item)

        val app = MoolVaniApplication.instance
        val textToSpeak = item.santhaliRoman.ifEmpty { item.santhaliOlchiki }
        app.audioEngine.playText(textToSpeak, isSanthali = true) {
            mainHandler.postDelayed({
                if (isPlayingSequence) {
                    playItemAt(currentIndex + 1)
                }
            }, 600)
        }
    }

    private fun displayActiveItem(item: CourseItem) {
        binding.tvReaderEmoji.text = item.iconEmoji
        binding.tvReaderOlchiki.text = item.santhaliOlchiki
        binding.tvReaderRoman.text = item.santhaliRoman
        binding.tvReaderHindi.text = "${item.hindiText} (${item.englishMeaning})"
    }

    private fun loadCourseItems() {
        val app = MoolVaniApplication.instance
        items = app.repository.getCourseItems(courseCode)

        if (items.isNotEmpty()) {
            displayActiveItem(items[0])
        }

        binding.rvCourseVocab.layoutManager = LinearLayoutManager(this)
        binding.rvCourseVocab.adapter = VocabularyAdapter(items) { item ->
            displayActiveItem(item)
            currentIndex = items.indexOf(item)
            val textToSpeak = item.santhaliRoman.ifEmpty { item.santhaliOlchiki }
            app.audioEngine.playText(textToSpeak, isSanthali = true)
        }
    }

    override fun onPause() {
        super.onPause()
        pauseSequence()
    }

    private inner class VocabularyAdapter(
        private val list: List<CourseItem>,
        private val onClick: (CourseItem) -> Unit
    ) : RecyclerView.Adapter<VocabularyAdapter.VocabViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VocabViewHolder {
            val b = ItemCourseVocabularyBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return VocabViewHolder(b)
        }

        override fun onBindViewHolder(holder: VocabViewHolder, position: Int) {
            val item = list[position]
            holder.binding.tvVocabEmoji.text = item.iconEmoji
            holder.binding.tvVocabHindi.text = item.hindiText
            holder.binding.tvVocabOlchiki.text = item.santhaliOlchiki
            holder.binding.tvVocabRoman.text = "${item.santhaliRoman} • ${item.englishMeaning}"

            holder.binding.btnVocabPlay.setOnClickListener {
                onClick(item)
            }
            holder.binding.root.setOnClickListener {
                onClick(item)
            }
        }

        override fun getItemCount(): Int = list.size

        inner class VocabViewHolder(val binding: ItemCourseVocabularyBinding) :
            RecyclerView.ViewHolder(binding.root)
    }
}

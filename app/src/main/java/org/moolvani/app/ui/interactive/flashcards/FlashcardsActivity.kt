package org.moolvani.app.ui.interactive.flashcards

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import org.moolvani.app.MoolVaniApplication
import org.moolvani.app.data.model.CourseItem
import org.moolvani.app.databinding.ActivityFlashcardsBinding

class FlashcardsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFlashcardsBinding
    private var cards: MutableList<CourseItem> = mutableListOf()
    private var currentIndex: Int = 0
    private var isFrontShowing: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFlashcardsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackFlashcards.setOnClickListener { finish() }

        setupControls()
        loadFlashcards()
    }

    private fun setupControls() {
        // Tap card to flip
        binding.cardFlipContainer.setOnClickListener { flipCard() }
        binding.btnFcFlip.setOnClickListener { flipCard() }

        // Next / Prev
        binding.btnFcNext.setOnClickListener { nextCard() }
        binding.btnFcPrev.setOnClickListener { prevCard() }

        // Shuffle
        binding.btnShuffleCards.setOnClickListener { shuffleCards() }

        // Play Audio
        binding.btnFcPlayAudio.setOnClickListener { playCurrentAudio() }
        binding.btnFcAudioQuick.setOnClickListener { playCurrentAudio() }
    }

    private fun loadFlashcards() {
        val app = MoolVaniApplication.instance
        cards = app.repository.getAllCourseItems().toMutableList()
        if (cards.isEmpty()) {
            cards.addAll(org.moolvani.app.data.seed.SeedData.initialCourseItems)
        }
        currentIndex = 0
        displayCard(currentIndex)
    }

    private fun displayCard(index: Int) {
        if (cards.isEmpty()) return
        val card = cards[index]

        isFrontShowing = true
        binding.cardFrontLayout.visibility = View.VISIBLE
        binding.cardBackLayout.visibility = View.GONE
        binding.cardFlipContainer.rotationY = 0f

        // Front Data: Image + Hindi + Category
        binding.tvFcFrontCategory.text = card.courseCode
        binding.tvFcFrontImage.text = card.iconEmoji
        binding.tvFcFrontHindi.text = card.hindiText
        binding.tvFcFrontMeaning.text = "(${card.englishMeaning})"

        // Back Data: Santhali Ol Chiki + Roman + Audio
        binding.tvFcBackCategory.text = "${card.courseCode} (ᱥᱟᱱᱛᱟᱲᱤ)"
        binding.tvFcBackOlchiki.text = card.santhaliOlchiki
        binding.tvFcBackRoman.text = card.santhaliRoman
        binding.tvFcBackHindiRef.text = "Hindi: ${card.hindiText} • ${card.englishMeaning}"

        binding.tvFcCounter.text = "Card ${index + 1} of ${cards.size}"
    }

    private fun flipCard() {
        val container = binding.cardFlipContainer
        val anim1 = ObjectAnimator.ofFloat(container, "rotationY", 0f, 90f).apply {
            duration = 180
            interpolator = AccelerateDecelerateInterpolator()
        }
        val anim2 = ObjectAnimator.ofFloat(container, "rotationY", -90f, 0f).apply {
            duration = 180
            interpolator = AccelerateDecelerateInterpolator()
        }

        anim1.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (isFrontShowing) {
                    binding.cardFrontLayout.visibility = View.GONE
                    binding.cardBackLayout.visibility = View.VISIBLE
                    isFrontShowing = false
                    // Auto-play audio when flipped to Santhali side
                    playCurrentAudio()
                } else {
                    binding.cardFrontLayout.visibility = View.VISIBLE
                    binding.cardBackLayout.visibility = View.GONE
                    isFrontShowing = true
                }
                anim2.start()
            }
        })

        anim1.start()
    }

    private fun nextCard() {
        if (cards.isEmpty()) return
        currentIndex = (currentIndex + 1) % cards.size
        displayCard(currentIndex)
    }

    private fun prevCard() {
        if (cards.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) cards.size - 1 else currentIndex - 1
        displayCard(currentIndex)
    }

    private fun shuffleCards() {
        cards.shuffle()
        currentIndex = 0
        displayCard(currentIndex)
    }

    private fun playCurrentAudio() {
        if (cards.isEmpty()) return
        val card = cards[currentIndex]
        val textToSpeak = card.santhaliRoman.ifEmpty { card.santhaliOlchiki }
        val app = MoolVaniApplication.instance
        app.audioEngine.playText(textToSpeak, isSanthali = true)
    }
}

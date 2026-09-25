package org.moolvani.app.ui.interactive.worksheets

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.moolvani.app.MoolVaniApplication
import org.moolvani.app.R
import org.moolvani.app.data.model.QuestionType
import org.moolvani.app.data.model.WorksheetQuestion
import org.moolvani.app.databinding.ActivityWorksheetBinding

class WorksheetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorksheetBinding
    private var questions: List<WorksheetQuestion> = emptyList()
    private var currentIndex: Int = 0
    private var score: Int = 0
    private var isAnswerChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorksheetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackWorksheet.setOnClickListener { finish() }
        binding.btnNewWorksheet.setOnClickListener { generateNewQuiz() }

        binding.btnActionQuiz.setOnClickListener {
            if (!isAnswerChecked) {
                checkAnswer()
            } else {
                nextQuestion()
            }
        }

        binding.btnListenQAudio.setOnClickListener {
            if (currentIndex < questions.size) {
                val q = questions[currentIndex]
                val textToSpeak = q.correctText.ifEmpty { q.promptSanthali }
                val app = MoolVaniApplication.instance
                app.audioEngine.playText(textToSpeak, isSanthali = true)
            }
        }

        generateNewQuiz()
    }

    private fun generateNewQuiz() {
        val app = MoolVaniApplication.instance
        questions = app.worksheetGenerator.generateWorksheet(count = 8)
        currentIndex = 0
        score = 0
        isAnswerChecked = false
        updateScoreUi()
        displayQuestion(currentIndex)
    }

    private fun updateScoreUi() {
        binding.tvScoreBadge.text = "Score: $score / ${questions.size}"
        if (questions.isNotEmpty()) {
            val progress = ((currentIndex + 1) * 100) / questions.size
            binding.progressQuiz.progress = progress
            binding.tvQCounter.text = "Question ${currentIndex + 1} of ${questions.size}"
        }
    }

    private fun displayQuestion(index: Int) {
        if (index >= questions.size) {
            showQuizCompleted()
            return
        }

        val q = questions[index]
        isAnswerChecked = false
        binding.btnActionQuiz.text = "Check Answer"
        binding.feedbackContainer.visibility = View.GONE

        binding.tvQTypeBadge.text = when (q.type) {
            QuestionType.PICTURE_BASED -> "चित्र पहचानिए (Picture Based)"
            QuestionType.MCQ -> "बहुविकल्पीय प्रश्न (Multiple Choice)"
            QuestionType.FILL_BLANK -> "रिक्त स्थान भरिए (Fill in Blank)"
            QuestionType.MATCHING -> "जोड़ी मिलाइए (Matching Pairs)"
        }

        binding.tvQPromptHindi.text = q.promptHindi
        binding.tvQPromptSanthali.text = q.promptSanthali

        if (q.type == QuestionType.PICTURE_BASED && q.imageEmoji.isNotEmpty()) {
            binding.tvQImageEmoji.visibility = View.VISIBLE
            binding.tvQImageEmoji.text = q.imageEmoji
        } else {
            binding.tvQImageEmoji.visibility = View.GONE
        }

        // Setup Options Container for MCQ / Picture / Fill Blank
        if (q.type != QuestionType.MATCHING) {
            binding.rgOptions.visibility = View.VISIBLE
            binding.matchingContainer.visibility = View.GONE
            binding.rgOptions.removeAllViews()

            for ((optIdx, option) in q.options.withIndex()) {
                val rb = RadioButton(this).apply {
                    id = View.generateViewId()
                    text = option
                    textSize = 16f
                    setTextColor(resources.getColor(R.color.navy_primary, null))
                    setPadding(16, 12, 16, 12)
                    tag = optIdx
                }
                binding.rgOptions.addView(rb)
            }
        } else {
            // Setup Matching layout
            binding.rgOptions.visibility = View.GONE
            binding.matchingContainer.visibility = View.VISIBLE
            setupMatchingLayout(q)
        }

        updateScoreUi()
    }

    private fun setupMatchingLayout(q: WorksheetQuestion) {
        val container = binding.matchingContainer
        container.removeAllViews()

        for (pair in q.matchingPairs) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }

            val tvHindi = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = "• ${pair.hindiText}"
                textSize = 15f
                setTextColor(resources.getColor(R.color.navy_primary, null))
            }

            val tvArrow = TextView(this).apply {
                text = " ➔ "
                textSize = 15f
                setTextColor(resources.getColor(R.color.text_secondary, null))
            }

            val tvOlchiki = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
                text = "${pair.santhaliOlchiki} (${pair.santhaliRoman})"
                textSize = 16f
                setTextColor(resources.getColor(R.color.navy_accent, null))
            }

            row.addView(tvHindi)
            row.addView(tvArrow)
            row.addView(tvOlchiki)
            container.addView(row)
        }
    }

    private fun checkAnswer() {
        if (currentIndex >= questions.size) return
        val q = questions[currentIndex]

        var isCorrect = false

        if (q.type == QuestionType.MATCHING) {
            // Matching pairs auto-evaluated on review
            isCorrect = true
        } else {
            val checkedId = binding.rgOptions.checkedRadioButtonId
            if (checkedId == -1) {
                Toast.makeText(this, "Please select an answer first", Toast.LENGTH_SHORT).show()
                return
            }
            val checkedRb = binding.rgOptions.findViewById<RadioButton>(checkedId)
            val selectedIdx = checkedRb.tag as? Int ?: -1
            isCorrect = (selectedIdx == q.correctIndex)
        }

        if (isCorrect) {
            score++
            binding.tvFeedbackTitle.text = "✅ शाबाश! बिल्कुल सही उत्तर! (Correct)"
            binding.tvFeedbackTitle.setTextColor(resources.getColor(R.color.success_green, null))
            binding.feedbackContainer.setBackgroundColor(0xFFDCFCE7.toInt())
        } else {
            binding.tvFeedbackTitle.text = "❌ पुनः प्रयास करें (Try Again)"
            binding.tvFeedbackTitle.setTextColor(resources.getColor(R.color.action_speak, null))
            binding.feedbackContainer.setBackgroundColor(0xFFFEE2E2.toInt())
        }

        binding.tvFeedbackExplanation.text = q.explanation
        binding.feedbackContainer.visibility = View.VISIBLE
        isAnswerChecked = true
        binding.btnActionQuiz.text = if (currentIndex + 1 < questions.size) "Next Question ➔" else "View Results ➔"

        // Play correct answer audio
        val app = MoolVaniApplication.instance
        val textToSpeak = q.correctText.ifEmpty { q.promptSanthali }
        app.audioEngine.playText(textToSpeak, isSanthali = true)

        updateScoreUi()
    }

    private fun nextQuestion() {
        currentIndex++
        if (currentIndex < questions.size) {
            displayQuestion(currentIndex)
        } else {
            showQuizCompleted()
        }
    }

    private fun showQuizCompleted() {
        binding.rgOptions.visibility = View.GONE
        binding.matchingContainer.visibility = View.GONE
        binding.tvQImageEmoji.visibility = View.VISIBLE
        binding.tvQImageEmoji.text = "🏆"
        binding.tvQTypeBadge.text = "ASSESSMENT COMPLETED"
        binding.tvQPromptHindi.text = "बधाई! आपने वर्कशीट पूरी कर ली है।"
        binding.tvQPromptSanthali.text = "ᱥᱟᱨᱦᱟᱣ! ᱟᱢ ᱠᱟᱹᱢᱤᱦᱚᱨᱟᱢ ᱯᱩᱨᱟᱹᱣ ᱠᱮᱫᱟ!"
        binding.tvFeedbackTitle.text = "Final Score: $score / ${questions.size} (${(score * 100) / questions.size.coerceAtLeast(1)}%)"
        binding.tvFeedbackTitle.setTextColor(resources.getColor(R.color.navy_primary, null))
        binding.tvFeedbackExplanation.text = "Excellent offline classroom pedagogy progress. You can generate a new randomized assessment anytime."
        binding.feedbackContainer.visibility = View.VISIBLE
        binding.btnActionQuiz.text = "Restart Worksheet 🔄"
        isAnswerChecked = true
        binding.btnActionQuiz.setOnClickListener {
            generateNewQuiz()
        }
    }
}

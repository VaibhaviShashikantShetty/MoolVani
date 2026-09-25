package org.moolvani.app.ui.phrases

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import org.moolvani.app.MoolVaniApplication
import org.moolvani.app.R
import org.moolvani.app.data.model.Phrase
import org.moolvani.app.databinding.FragmentPhrasesBinding
import org.moolvani.app.engine.OlChikiTransliterator

class PhrasesFragment : Fragment() {

    private var _binding: FragmentPhrasesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PhraseAdapter
    private var allPhrases: List<Phrase> = emptyList()
    private var selectedCategory: String = "All"

    private val categories = listOf(
        "All",
        "Classroom Commands",
        "Questions",
        "Student Responses",
        "Greetings"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhrasesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupCategoryChips()
        setupSearch()
        setupAddPhraseButton()
        loadPhrases()
    }

    private fun setupRecyclerView() {
        // 2 columns for phones, 3 columns for tablet/wide screens
        val spanCount = if (resources.configuration.screenWidthDp >= 600) 3 else 2
        binding.rvPhrases.layoutManager = GridLayoutManager(requireContext(), spanCount)

        adapter = PhraseAdapter(
            phrases = emptyList(),
            onPlayAudio = { phrase ->
                playPhraseAudio(phrase)
            },
            onItemClick = { phrase ->
                playPhraseAudio(phrase)
                Toast.makeText(
                    requireContext(),
                    "${phrase.hindiText} ➔ ${phrase.santhaliOlchiki} (${phrase.santhaliRoman})",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        binding.rvPhrases.adapter = adapter
    }

    private fun playPhraseAudio(phrase: Phrase) {
        val app = MoolVaniApplication.instance
        val textToSpeak = phrase.santhaliRoman.ifEmpty { phrase.santhaliOlchiki }
        app.audioEngine.playText(textToSpeak, isSanthali = true)
    }

    private fun setupCategoryChips() {
        val container = binding.categoryChipsContainer
        container.removeAllViews()

        for (cat in categories) {
            val chip = Button(requireContext(), null, 0, androidx.appcompat.R.style.Widget_AppCompat_Button_Borderless).apply {
                text = cat
                textSize = 12f
                val isSelected = (cat == selectedCategory)
                setTextColor(resources.getColor(if (isSelected) R.color.white else R.color.navy_primary, null))
                setBackgroundResource(if (isSelected) R.drawable.bg_pill_button else R.drawable.bg_pill_outline)
                setPadding(28, 0, 28, 0)
                val params = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    resources.getDimensionPixelSize(androidx.appcompat.R.dimen.abc_action_button_min_height_material)
                ).apply {
                    setMargins(8, 4, 8, 4)
                }
                layoutParams = params
                setOnClickListener {
                    selectedCategory = cat
                    setupCategoryChips()
                    filterPhrases()
                }
            }
            container.addView(chip)
        }
    }

    private fun setupSearch() {
        binding.etSearchPhrases.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                filterPhrases()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadPhrases() {
        val app = MoolVaniApplication.instance
        allPhrases = app.repository.getAllPhrases()
        filterPhrases()
    }

    private fun filterPhrases() {
        val query = binding.etSearchPhrases.text.toString().trim()
        var filtered = if (selectedCategory == "All") {
            allPhrases
        } else {
            allPhrases.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }

        if (query.isNotEmpty()) {
            val lower = query.lowercase()
            filtered = filtered.filter {
                it.hindiText.lowercase().contains(lower) ||
                it.santhaliOlchiki.contains(query) ||
                it.santhaliRoman.lowercase().contains(lower)
            }
        }

        adapter.updateData(filtered)
    }

    private fun setupAddPhraseButton() {
        binding.btnAddPhrase.setOnClickListener {
            showAddPhraseDialog()
        }
    }

    private fun showAddPhraseDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_phrase, null)
        val etHindi = dialogView.findViewById<EditText>(R.id.et_dialog_hindi)
        val etOlchiki = dialogView.findViewById<EditText>(R.id.et_dialog_olchiki)
        val etRoman = dialogView.findViewById<EditText>(R.id.et_dialog_roman)
        val etCategory = dialogView.findViewById<EditText>(R.id.et_dialog_category)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_dialog_cancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_dialog_save)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val hindi = etHindi.text.toString().trim()
            if (hindi.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter Hindi text", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var olchiki = etOlchiki.text.toString().trim()
            var roman = etRoman.text.toString().trim()
            val cat = etCategory.text.toString().trim().ifEmpty { "Classroom Commands" }

            if (olchiki.isEmpty()) {
                olchiki = OlChikiTransliterator.toOlChiki(hindi)
            }
            if (roman.isEmpty()) {
                roman = hindi
            }

            val newPhrase = Phrase(
                hindiText = hindi,
                santhaliOlchiki = olchiki,
                santhaliRoman = roman,
                englishMeaning = "Classroom phrase",
                category = cat,
                isCustom = true
            )

            val app = MoolVaniApplication.instance
            app.repository.insertPhrase(newPhrase)
            app.translationEngine.syncFromDatabase()
            loadPhrases()
            dialog.dismiss()
            Toast.makeText(requireContext(), "New phrase added successfully! 🎉", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        loadPhrases()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

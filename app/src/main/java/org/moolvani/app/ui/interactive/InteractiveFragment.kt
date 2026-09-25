package org.moolvani.app.ui.interactive

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.moolvani.app.databinding.FragmentInteractiveBinding
import org.moolvani.app.ui.interactive.flashcards.FlashcardsActivity
import org.moolvani.app.ui.interactive.worksheets.WorksheetActivity

class InteractiveFragment : Fragment() {

    private var _binding: FragmentInteractiveBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInteractiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Module 1: Worksheet and Assessment
        binding.cardWorksheets.setOnClickListener {
            startActivity(Intent(requireContext(), WorksheetActivity::class.java))
        }
        binding.btnLaunchWorksheet.setOnClickListener {
            startActivity(Intent(requireContext(), WorksheetActivity::class.java))
        }

        // Module 2: Flash Cards
        binding.cardFlashcards.setOnClickListener {
            startActivity(Intent(requireContext(), FlashcardsActivity::class.java))
        }
        binding.btnLaunchFlashcards.setOnClickListener {
            startActivity(Intent(requireContext(), FlashcardsActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

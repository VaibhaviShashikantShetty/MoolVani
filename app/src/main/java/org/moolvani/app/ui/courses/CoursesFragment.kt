package org.moolvani.app.ui.courses

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import org.moolvani.app.MoolVaniApplication
import org.moolvani.app.R
import org.moolvani.app.data.model.Course
import org.moolvani.app.data.model.CourseItem
import org.moolvani.app.data.model.Language
import org.moolvani.app.databinding.FragmentCoursesBinding

class CoursesFragment : Fragment() {

    private var _binding: FragmentCoursesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CourseAdapter
    private var courses: List<Course> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoursesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupUploadButton()
        loadCourses()
    }

    private fun setupRecyclerView() {
        // 2 columns for tablets/phones
        binding.rvCourses.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = CourseAdapter(
            courses = emptyList(),
            onExploreClick = { course ->
                val intent = Intent(requireContext(), CourseDetailActivity::class.java).apply {
                    putExtra("EXTRA_COURSE_CODE", course.code)
                    putExtra("EXTRA_COURSE_TITLE", "${course.code} (${course.titleOlchiki})")
                    putExtra("EXTRA_COURSE_EMOJI", course.iconEmoji)
                }
                startActivity(intent)
            }
        )
        binding.rvCourses.adapter = adapter
    }

    private fun loadCourses() {
        val app = MoolVaniApplication.instance
        courses = app.repository.getAllCourses()
        adapter.updateData(courses)
    }

    private fun setupUploadButton() {
        binding.btnUploadMaterial.setOnClickListener {
            showUploadDialog()
        }
    }

    private fun showUploadDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_upload_material, null)
        val spinnerCourse = dialogView.findViewById<Spinner>(R.id.spinner_course_target)
        val etHindi = dialogView.findViewById<EditText>(R.id.et_upload_hindi)
        val etOlchiki = dialogView.findViewById<EditText>(R.id.et_upload_olchiki)
        val etEmoji = dialogView.findViewById<EditText>(R.id.et_upload_emoji)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_upload_cancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_upload_save)

        val courseCodes = courses.map { it.code }
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, courseCodes)
        spinnerCourse.adapter = spinnerAdapter

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

            val app = MoolVaniApplication.instance
            val selectedCourse = spinnerCourse.selectedItem?.toString() ?: "ANIMALS"
            var olchiki = etOlchiki.text.toString().trim()
            val emoji = etEmoji.text.toString().trim().ifEmpty { "📚" }

            // Auto-translate if ol chiki empty
            var roman = hindi
            if (olchiki.isEmpty()) {
                val translation = app.translationEngine.translate(hindi, Language.HINDI, Language.SANTHALI)
                olchiki = translation.targetOlChiki
                roman = translation.targetRoman
            }

            val newItem = CourseItem(
                courseCode = selectedCourse,
                hindiText = hindi,
                santhaliOlchiki = olchiki,
                santhaliRoman = roman,
                englishMeaning = "Teaching vocabulary",
                iconEmoji = emoji
            )

            app.repository.insertCourseItem(newItem)
            loadCourses()
            dialog.dismiss()
            Toast.makeText(requireContext(), "Material translated & stored offline! 🚀", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        loadCourses()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

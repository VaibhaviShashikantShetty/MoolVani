package org.moolvani.app.ui.courses

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.moolvani.app.data.model.Course
import org.moolvani.app.databinding.ItemCourseCardBinding

class CourseAdapter(
    private var courses: List<Course>,
    private val onExploreClick: (Course) -> Unit
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    fun updateData(newCourses: List<Course>) {
        courses = newCourses
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding = ItemCourseCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CourseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(courses[position])
    }

    override fun getItemCount(): Int = courses.size

    inner class CourseViewHolder(private val binding: ItemCourseCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course) {
            binding.tvCourseEmoji.text = course.iconEmoji
            binding.tvCourseCode.text = course.code
            binding.tvCourseBilingual.text = "${course.titleHi} • ${course.titleOlchiki}"
            binding.tvCourseDesc.text = course.description
            binding.tvCourseItemsCount.text = "${course.itemCount} Items"

            binding.btnCourseExplore.setOnClickListener {
                onExploreClick(course)
            }

            binding.root.setOnClickListener {
                onExploreClick(course)
            }
        }
    }
}

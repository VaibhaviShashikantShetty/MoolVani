package org.moolvani.app.ui.phrases

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.moolvani.app.data.model.Phrase
import org.moolvani.app.databinding.ItemPhraseCardBinding

class PhraseAdapter(
    private var phrases: List<Phrase>,
    private val onPlayAudio: (Phrase) -> Unit,
    private val onItemClick: (Phrase) -> Unit
) : RecyclerView.Adapter<PhraseAdapter.PhraseViewHolder>() {

    fun updateData(newPhrases: List<Phrase>) {
        phrases = newPhrases
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhraseViewHolder {
        val binding = ItemPhraseCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhraseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhraseViewHolder, position: Int) {
        holder.bind(phrases[position])
    }

    override fun getItemCount(): Int = phrases.size

    inner class PhraseViewHolder(private val binding: ItemPhraseCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(phrase: Phrase) {
            binding.tvPhraseHindi.text = phrase.hindiText
            binding.tvPhraseOlchiki.text = phrase.santhaliOlchiki
            binding.tvPhraseRoman.text = phrase.santhaliRoman
            binding.tvCardCategory.text = phrase.category

            binding.btnCardPlayAudio.setOnClickListener {
                onPlayAudio(phrase)
            }

            binding.root.setOnClickListener {
                onItemClick(phrase)
            }
        }
    }
}

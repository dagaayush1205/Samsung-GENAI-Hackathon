package com.google.mediapipe.examples.poselandmarker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.mediapipe.examples.poselandmarker.data.WorkoutSession
import com.google.mediapipe.examples.poselandmarker.databinding.ItemHistoryEntryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter : ListAdapter<WorkoutSession, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback) {

    class HistoryViewHolder(private val binding: ItemHistoryEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(session: WorkoutSession) {
            binding.exerciseTypeText.text = session.exerciseType
            binding.dateText.text = dateFormat.format(session.date)
            binding.repsText.text = "${session.reps} Reps"
            binding.scoreText.text = "${session.score}% Accuracy"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

object HistoryDiffCallback : DiffUtil.ItemCallback<WorkoutSession>() {
    override fun areItemsTheSame(oldItem: WorkoutSession, newItem: WorkoutSession): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: WorkoutSession, newItem: WorkoutSession): Boolean {
        return oldItem == newItem
    }
}

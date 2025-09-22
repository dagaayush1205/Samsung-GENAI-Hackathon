package com.google.mediapipe.examples.poselandmarker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.mediapipe.examples.poselandmarker.data.AppDatabase
import com.google.mediapipe.examples.poselandmarker.databinding.ActivityHistoryBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = HistoryAdapter()
        binding.historyRecyclerView.adapter = adapter

        val dao = AppDatabase.getDatabase(this).workoutDao()

        lifecycleScope.launch {
            dao.getAllSessions().collect { sessions ->
                adapter.submitList(sessions)
            }
        }
    }
}


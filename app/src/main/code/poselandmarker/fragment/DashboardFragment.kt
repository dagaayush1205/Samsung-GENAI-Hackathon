package com.google.mediapipe.examples.poselandmarker.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mediapipe.examples.poselandmarker.R
import com.google.mediapipe.examples.poselandmarker.databinding.FragmentDashboardBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDate()
        setupCalendar()
    }

    private fun setupDate() {
        val dateFormat = SimpleDateFormat("MMMM yy", Locale.getDefault())
        val today = dateFormat.format(Date())
        binding.dateTextView.text = today
    }

    private fun setupCalendar() {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)

        val days = listOf("M", "T", "W", "T", "F", "S", "S")

        // Correctly access the TextViews inside the included layouts
        val dayViews = listOf(
            binding.day1Container.day, binding.day2Container.day, binding.day3Container.day,
            binding.day4Container.day, binding.day5Container.day, binding.day6Container.day,
            binding.day7Container.day
        )
        val dateViews = listOf(
            binding.day1Container.date, binding.day2Container.date, binding.day3Container.date,
            binding.day4Container.date, binding.day5Container.date, binding.day6Container.date,
            binding.day7Container.date
        )

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // SUNDAY = 1, MONDAY = 2...
        val startOffset = if (dayOfWeek == 1) 6 else dayOfWeek - 2 // Adjust to make Monday the start

        for (i in 0..6) {
            dayViews[i].text = days[i]
            val dayOfMonth = today - startOffset + i
            dateViews[i].text = dayOfMonth.toString()

            // Highlight today's date
            if (i == startOffset) {
                dateViews[i].setBackgroundResource(R.drawable.calendar_today_background)
                dateViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_accent))
                dayViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_lime))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

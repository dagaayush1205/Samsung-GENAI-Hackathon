package com.google.mediapipe.examples.poselandmarker.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.mediapipe.examples.poselandmarker.WorkoutActivity
import com.google.mediapipe.examples.poselandmarker.databinding.FragmentWorkoutsBinding

class WorkoutsFragment : Fragment() {

    private var _binding: FragmentWorkoutsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.startAiCoachButton.setOnClickListener {
            val intent = Intent(activity, WorkoutActivity::class.java)
            startActivity(intent)
        }
      // Inside WorkoutsFragment.kt's onViewCreated function

        binding.startAiCoachButton.setOnClickListener {
            val intent = Intent(activity, WorkoutActivity::class.java)
            // Tell the camera screen we want to do push-ups
            intent.putExtra("WORKOUT_TYPE", "PUSH_UP")
            startActivity(intent)
        }
        
        // --- ADD THIS NEW CLICK LISTENER ---
        binding.startSquatCoachButton.setOnClickListener {
            val intent = Intent(activity, WorkoutActivity::class.java)
            // Tell the camera screen we want to do squats
            intent.putExtra("WORKOUT_TYPE", "SQUAT")
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


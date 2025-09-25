package com.google.mediapipe.examples.poselandmarker.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.mediapipe.examples.poselandmarker.HistoryActivity
import com.google.mediapipe.examples.poselandmarker.data.AppDatabase
import com.google.mediapipe.examples.poselandmarker.databinding.FragmentProfileBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                fetchProfileName()
            } else {
                binding.profileName.text = "User"
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermissionAndFetchProfile()
        observeProfileStats() // Call the new function to load stats

        binding.historyButton.setOnClickListener {
            val intent = Intent(activity, HistoryActivity::class.java)
            startActivity(intent)
        }
    }

    // --- NEW FUNCTION TO FETCH AND DISPLAY STATS FROM THE DATABASE ---
    private fun observeProfileStats() {
        val dao = AppDatabase.getDatabase(requireContext()).workoutDao()

        // Use lifecycleScope to automatically listen for database changes
        lifecycleScope.launch {
            dao.getAllSessions().collect { sessions ->
                // Calculate the stats
                val totalReps = sessions.sumOf { it.reps }
                val totalWorkouts = sessions.size

                // Update the UI
                binding.totalRepsText.text = totalReps.toString()
                binding.totalWorkoutsText.text = totalWorkouts.toString()
            }
        }
    }

    private fun checkPermissionAndFetchProfile() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                fetchProfileName()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun fetchProfileName() {
        val projection = arrayOf(ContactsContract.Profile.DISPLAY_NAME_PRIMARY)
        val cursor = requireActivity().contentResolver.query(
            ContactsContract.Profile.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        var profileName = "User" // Default name
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME_PRIMARY)
                if(nameIndex > -1) {
                    val name = it.getString(nameIndex)
                    if (!name.isNullOrEmpty()) {
                        profileName = name
                    }
                }
            }
        }
        binding.profileName.text = profileName
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

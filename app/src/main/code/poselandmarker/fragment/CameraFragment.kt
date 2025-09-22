/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.poselandmarker.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.google.mediapipe.examples.poselandmarker.MainViewModel
import com.google.mediapipe.examples.poselandmarker.PoseLandmarkerHelper
import com.google.mediapipe.examples.poselandmarker.R
import com.google.mediapipe.examples.poselandmarker.data.AppDatabase
import com.google.mediapipe.examples.poselandmarker.data.Challenge
import com.google.mediapipe.examples.poselandmarker.data.WorkoutSession
import com.google.mediapipe.examples.poselandmarker.databinding.FragmentCameraBinding
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.atan2

class CameraFragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener, TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "Pose Landmarker"
    }

    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding get() = _fragmentCameraBinding!!

    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT

    private lateinit var backgroundExecutor: ExecutorService
    private lateinit var textToSpeech: TextToSpeech

    private var currentWorkoutType: String = "PUSH_UP"
    private var repCount = 0
    private var workoutState = "up"
    private var formScoreAccumulator = 0.0
    private var formScoreSamples = 0
    private var lastFeedbackTime = 0L

    private val dailyChallenges = listOf(
        Challenge("Quick 15", "Do 15 push-ups in a single session.", 15, "Push-ups"),
        Challenge("Morning Burst", "Get your blood pumping with 20 push-ups.", 20, "Push-ups"),
        Challenge("Solid Strength", "Show your strength with 25 push-ups.", 25, "Push-ups"),
        Challenge("The Challenger", "Push your limits with 30 push-ups.", 30, "Push-ups"),
        Challenge("Endurance Test", "Go the distance with 35 push-ups.", 35, "Push-ups")
    )

    override fun onResume() {
        super.onResume()
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(
                requireActivity(), R.id.fragment_container
            ).navigate(R.id.action_camera_to_permissions)
        }

        backgroundExecutor.execute {
            if (this::poseLandmarkerHelper.isInitialized) {
                if (poseLandmarkerHelper.isClose()) {
                    poseLandmarkerHelper.setupPoseLandmarker()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::poseLandmarkerHelper.isInitialized) {
            viewModel.setMinPoseDetectionConfidence(poseLandmarkerHelper.minPoseDetectionConfidence)
            viewModel.setMinPoseTrackingConfidence(poseLandmarkerHelper.minPoseTrackingConfidence)
            viewModel.setMinPosePresenceConfidence(poseLandmarkerHelper.minPosePresenceConfidence)
            viewModel.setDelegate(poseLandmarkerHelper.currentDelegate)
            backgroundExecutor.execute { poseLandmarkerHelper.clearPoseLandmarker() }
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
        if (this::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        currentWorkoutType = activity?.intent?.getStringExtra("WORKOUT_TYPE") ?: "PUSH_UP"
        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backgroundExecutor = Executors.newSingleThreadExecutor()

        fragmentCameraBinding.viewFinder.post {
            setUpCamera() // This call is now valid
        }

        backgroundExecutor.execute {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                currentDelegate = viewModel.currentDelegate,
                poseLandmarkerHelperListener = this
            )
        }

        textToSpeech = TextToSpeech(requireContext(), this)

        fragmentCameraBinding.finishWorkoutButton.setOnClickListener {
            val finalScore = if (formScoreSamples > 0) (formScoreAccumulator / formScoreSamples).toInt() else 100
            val exerciseName = if (currentWorkoutType == "SQUAT") "Squats" else "Push-ups"
            saveWorkoutSession(exerciseName, repCount, finalScore)
            activity?.finish()
        }
    }

    // --- ALL THE MISSING CAMERA FUNCTIONS ARE RESTORED HERE ---
    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        detectPose(image)
                    }
                }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectPose(imageProxy: ImageProxy) {
        if (this::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
            )
        }
    }

    // --- All other functions from before are here ---

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "The Language specified is not supported!")
            }
        } else {
            Log.e(TAG, "TTS Initialization Failed!")
        }
    }

    private fun speak(text: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFeedbackTime > 2000) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
            lastFeedbackTime = currentTime
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding.viewFinder.display.rotation
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (_fragmentCameraBinding != null) {
                fragmentCameraBinding.overlay.setResults(
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )

                if (resultBundle.results.first().landmarks().isNotEmpty()) {
                    val formResult = analyzePose(resultBundle.results.first())
                    formScoreAccumulator += if (formResult.isCorrect) 100.0 else 0.0
                    formScoreSamples++

                    if (formResult.isCorrect) {
                        fragmentCameraBinding.aiFeedbackText.visibility = View.GONE
                    } else {
                        fragmentCameraBinding.aiFeedbackText.text = formResult.feedbackMessage
                        fragmentCameraBinding.aiFeedbackText.visibility = View.VISIBLE
                        speak(formResult.feedbackMessage)
                    }
                }
                fragmentCameraBinding.overlay.invalidate()
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun analyzePose(poseLandmarkerResult: PoseLandmarkerResult): FormResult {
        return when (currentWorkoutType) {
            "SQUAT" -> checkSquatFormAndCountReps(poseLandmarkerResult)
            else -> checkPushupFormAndCountReps(poseLandmarkerResult)
        }
    }

    private fun getAngle(
        p1: NormalizedLandmark,
        p2: NormalizedLandmark,
        p3: NormalizedLandmark
    ): Double {
        val angle = Math.toDegrees(
            (atan2(p3.y() - p2.y(), p3.x() - p2.x()) -
                    atan2(p1.y() - p2.y(), p1.x() - p2.x())).toDouble()
        )
        return if (angle < 0) angle + 360 else angle
    }

    private fun checkSquatFormAndCountReps(poseLandmarkerResult: PoseLandmarkerResult): FormResult {
        val landmarks = poseLandmarkerResult.landmarks().first()
        val leftShoulder = landmarks[11]
        val leftHip = landmarks[23]
        val leftKnee = landmarks[25]
        val leftAnkle = landmarks[27]
        val kneeAngle = getAngle(leftHip, leftKnee, leftAnkle)
        val hipAngle = getAngle(leftShoulder, leftHip, leftKnee)

        if (kneeAngle < 100 && hipAngle < 100) {
            if (workoutState == "up") {
                workoutState = "down"
            }
        } else if (kneeAngle > 160 && hipAngle > 170) {
            if (workoutState == "down") {
                repCount++
                fragmentCameraBinding.repCountText.text = "Reps: $repCount"
                workoutState = "up"
            }
        }
        if (kneeAngle > 100 && workoutState == "down") {
            if (hipAngle < 150) {
                 return FormResult(false, "Keep your chest up!")
            }
        }
        return FormResult(isCorrect = true, feedbackMessage = "Good squat!")
    }

    private fun checkPushupFormAndCountReps(poseLandmarkerResult: PoseLandmarkerResult): FormResult {
        val landmarks = poseLandmarkerResult.landmarks().first()
        val leftShoulder = landmarks[11]
        val leftElbow = landmarks[13]
        val leftWrist = landmarks[15]
        val leftHip = landmarks[23]
        val leftKnee = landmarks[25]
        val elbowAngle = getAngle(leftShoulder, leftElbow, leftWrist)
        val hipAngle = getAngle(leftShoulder, leftHip, leftKnee)

        if (elbowAngle < 90 && hipAngle > 150) {
            if (workoutState == "up") {
                workoutState = "down"
            }
        } else if (elbowAngle > 160 && hipAngle > 150) {
            if (workoutState == "down") {
                repCount++
                fragmentCameraBinding.repCountText.text = "Reps: $repCount"
                workoutState = "up"
            }
        }
        if (hipAngle < 150) {
            return FormResult(false, "Keep your back straight! Don't drop your hips.")
        }
        if (hipAngle > 195) {
            return FormResult(false, "Keep your back straight! Don't raise your hips.")
        }
        return FormResult(isCorrect = true, feedbackMessage = "Great Form!")
    }

    data class FormResult(val isCorrect: Boolean, val feedbackMessage: String)

    private fun saveWorkoutSession(exercise: String, reps: Int, score: Int) {
        val dao = AppDatabase.getDatabase(requireContext()).workoutDao()
        val session = WorkoutSession(
            date = Date(),
            exerciseType = exercise,
            reps = reps,
            score = score
        )
        lifecycleScope.launch(Dispatchers.IO) {
            dao.insert(session)
            val calendar = Calendar.getInstance()
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            val challengeIndex = dayOfYear % dailyChallenges.size
            val todayChallenge = dailyChallenges[challengeIndex]
            if (session.reps >= todayChallenge.repGoal && session.exerciseType.equals(todayChallenge.exerciseType, ignoreCase = true)) {
                val sharedPref = activity?.getSharedPreferences("WorkoutPrefs", Context.MODE_PRIVATE) ?: return@launch
                with(sharedPref.edit()) {
                    putInt("LastChallengeCompletedDay", dayOfYear)
                    apply()
                }
            }
        }
    }
}

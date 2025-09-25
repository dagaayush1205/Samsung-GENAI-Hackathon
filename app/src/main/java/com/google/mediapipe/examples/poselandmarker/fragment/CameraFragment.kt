/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 * This is the final, complete version with a robust, hybrid on-device AI system.
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
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.atan2

class CameraFragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener, TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "Pose Landmarker"
        const val MODEL_NAME = "llm_model.tflite"
    }

    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding get() = _fragmentCameraBinding!!

    private var llmInference: LlmInference? = null
    private var isLlmReady = false
    private var isProcessingLlm = false

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

    private val dailyChallenges = listOf(
        Challenge("Quick 15", "Do 15 push-ups in a single session.", 15, "Push-ups"),
        Challenge("Morning Burst", "Get your blood pumping with 20 push-ups.", 20, "Push-ups")
    )

    override fun onResume() {
        super.onResume()
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(R.id.action_camera_to_permissions)
        }
        backgroundExecutor.execute {
            if (this::poseLandmarkerHelper.isInitialized) {
                poseLandmarkerHelper.resume()
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
            backgroundExecutor.execute {
                poseLandmarkerHelper.clearPoseLandmarker()
            }
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        if (this::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        llmInference?.close()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        currentWorkoutType = activity?.intent?.getStringExtra("WORKOUT_TYPE") ?: "PUSH_UP"
        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backgroundExecutor = Executors.newSingleThreadExecutor()

        fragmentCameraBinding.viewFinder.post { setUpCamera() }

        backgroundExecutor.execute {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                context = requireContext(),
                minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                currentDelegate = viewModel.currentDelegate,
                poseLandmarkerHelperListener = this
            )
        }

        textToSpeech = TextToSpeech(requireContext(), this)

        fragmentCameraBinding.activateAiButton.setOnClickListener {
            it.visibility = View.GONE
            Toast.makeText(requireContext(), "Initializing AI Coach...", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                initializeLlm()
            }
        }

        fragmentCameraBinding.finishWorkoutButton.setOnClickListener {
            val exerciseName = if (currentWorkoutType == "SQUAT") "Squats" else "Push-ups"
            saveWorkoutSession(exerciseName, repCount, 100)
            activity?.finish()
        }
    }

    private suspend fun initializeLlm() {
        withContext(Dispatchers.IO) {
            try {
                val modelFile = File(requireContext().filesDir, MODEL_NAME)
                if (!modelFile.exists()) {
                    requireContext().assets.open(MODEL_NAME).use { input ->
                        modelFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                val options = LlmInference.LlmInferenceOptions.builder().setModelPath(modelFile.absolutePath).build()
                llmInference = LlmInference.createFromOptions(requireContext(), options)
                isLlmReady = true
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Generative AI Coach is active!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isLlmReady = false
                Log.e(TAG, "LLM Initialization failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "On-device AI failed. Using standard feedback.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (_fragmentCameraBinding == null || isProcessingLlm) return@runOnUiThread
            fragmentCameraBinding.overlay.setResults(resultBundle.results.first(), resultBundle.inputImageHeight, resultBundle.inputImageWidth, RunningMode.LIVE_STREAM)
            if (resultBundle.results.first().landmarks().isNotEmpty()) {
                val landmarks = resultBundle.results.first()
                analyzePose(landmarks)
            }
            fragmentCameraBinding.overlay.invalidate()
        }
    }

    private fun analyzePose(poseLandmarkerResult: PoseLandmarkerResult) {
        val repCompleted = when (currentWorkoutType) {
            "SQUAT" -> didCompleteSquatRep(poseLandmarkerResult)
            else -> didCompletePushupRep(poseLandmarkerResult)
        }

        if (repCompleted) {
            if (isLlmReady) {
                generateFeedbackWithLlm(poseLandmarkerResult)
            } else {
                val formResult = analyzePoseWithRules(poseLandmarkerResult)
                if (!formResult.isCorrect) {
                    fragmentCameraBinding.aiFeedbackText.text = formResult.feedbackMessage
                    fragmentCameraBinding.aiFeedbackText.visibility = View.VISIBLE
                    speak(formResult.feedbackMessage)
                } else {
                    fragmentCameraBinding.aiFeedbackText.visibility = View.GONE
                }
            }
        }
    }

    private fun generateFeedbackWithLlm(landmarks: PoseLandmarkerResult) {
        if (isProcessingLlm) return
        isProcessingLlm = true
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                poseLandmarkerHelper.pause()
            }
            val angles = getAnglesForWorkout(landmarks)
            val prompt = createPrompt(angles)
            val feedback = llmInference?.generateResponse(prompt) ?: "Keep going!"
            withContext(Dispatchers.Main) {
                fragmentCameraBinding.aiFeedbackText.text = feedback
                fragmentCameraBinding.aiFeedbackText.visibility = View.VISIBLE
                speak(feedback)
                poseLandmarkerHelper.resume()
                isProcessingLlm = false
            }
        }
    }

    private fun createPrompt(angles: Map<String, Double>): String {
        val exerciseName = if (currentWorkoutType == "SQUAT") "squat" else "push-up"
        val promptBuilder = StringBuilder()
        promptBuilder.append("User is doing a $exerciseName. ")
        angles.forEach { (name, angle) ->
            promptBuilder.append("$name angle is ${angle.toInt()} degrees. ")
        }
        promptBuilder.append("Provide short, encouraging, actionable feedback. If form is good, say 'Great form!'.")
        return promptBuilder.toString()
    }

    data class FormResult(val isCorrect: Boolean, val feedbackMessage: String)

    private fun analyzePoseWithRules(poseLandmarkerResult: PoseLandmarkerResult): FormResult {
        return when (currentWorkoutType) {
            "SQUAT" -> checkSquatForm(poseLandmarkerResult)
            else -> checkPushupForm(poseLandmarkerResult)
        }
    }

    // --- FULLY IMPLEMENTED LOGIC ---
    private fun didCompleteSquatRep(poseLandmarkerResult: PoseLandmarkerResult): Boolean {
        val landmarks = poseLandmarkerResult.landmarks().first()
        val leftShoulder = landmarks[11]; val leftHip = landmarks[23]; val leftKnee = landmarks[25]; val leftAnkle = landmarks[27]
        val kneeAngle = getAngle(leftHip, leftKnee, leftAnkle); val hipAngle = getAngle(leftShoulder, leftHip, leftKnee)
        
        if (kneeAngle < 100 && hipAngle < 100) {
            if (workoutState == "up") workoutState = "down"
        } else if (kneeAngle > 160 && hipAngle > 170) {
            if (workoutState == "down") {
                repCount++; fragmentCameraBinding.repCountText.text = "Reps: $repCount"; workoutState = "up"; return true
            }
        }
        return false
    }

    // --- FULLY IMPLEMENTED LOGIC ---
    private fun didCompletePushupRep(poseLandmarkerResult: PoseLandmarkerResult): Boolean {
        val landmarks = poseLandmarkerResult.landmarks().first()
        val leftShoulder = landmarks[11]; val leftElbow = landmarks[13]; val leftWrist = landmarks[15]; val leftHip = landmarks[23]; val leftKnee = landmarks[25]
        val elbowAngle = getAngle(leftShoulder, leftElbow, leftWrist); val hipAngle = getAngle(leftShoulder, leftHip, leftKnee)
        
        if (elbowAngle < 90 && hipAngle > 150) {
            if (workoutState == "up") workoutState = "down"
        } else if (elbowAngle > 160 && hipAngle > 150) {
            if (workoutState == "down") {
                repCount++; fragmentCameraBinding.repCountText.text = "Reps: $repCount"; workoutState = "up"; return true
            }
        }
        return false
    }

    // --- FULLY IMPLEMENTED LOGIC ---
    private fun checkSquatForm(poseLandmarkerResult: PoseLandmarkerResult): FormResult {
        val landmarks = poseLandmarkerResult.landmarks().first()
        val leftShoulder = landmarks[11]; val leftHip = landmarks[23]; val leftKnee = landmarks[25]; val leftAnkle = landmarks[27]
        val kneeAngle = getAngle(leftHip, leftKnee, leftAnkle); val hipAngle = getAngle(leftShoulder, leftHip, leftKnee)
        
        if (kneeAngle > 100 && workoutState == "down") {
            if (hipAngle < 150) {
                return FormResult(false, "Keep your chest up!")
            }
        }
        return FormResult(isCorrect = true, feedbackMessage = "Good squat!")
    }

    // --- FULLY IMPLEMENTED LOGIC ---
    private fun checkPushupForm(poseLandmarkerResult: PoseLandmarkerResult): FormResult {
        val landmarks = poseLandmarkerResult.landmarks().first()
        val leftShoulder = landmarks[11]; val leftElbow = landmarks[13]; val leftWrist = landmarks[15]; val leftHip = landmarks[23]; val leftKnee = landmarks[25]
        val elbowAngle = getAngle(leftShoulder, leftElbow, leftWrist); val hipAngle = getAngle(leftShoulder, leftHip, leftKnee)

        if (hipAngle < 150) return FormResult(false, "Keep your back straight! Don't drop your hips.")
        if (hipAngle > 195) return FormResult(false, "Keep your back straight! Don't raise your hips.")
        return FormResult(isCorrect = true, feedbackMessage = "Great Form!")
    }

    private fun getAnglesForWorkout(poseLandmarkerResult: PoseLandmarkerResult): Map<String, Double> {
        val landmarks = poseLandmarkerResult.landmarks().first()
        return when (currentWorkoutType) {
            "SQUAT" -> mapOf("Knee" to getAngle(landmarks[23], landmarks[25], landmarks[27]), "Hip" to getAngle(landmarks[11], landmarks[23], landmarks[25]))
            else -> mapOf("Elbow" to getAngle(landmarks[11], landmarks[13], landmarks[15]), "Hip" to getAngle(landmarks[11], landmarks[23], landmarks[25]))
        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext()); cameraProviderFuture.addListener({ cameraProvider = cameraProviderFuture.get(); bindCameraUseCases() }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed."); val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraFacing).build()
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation).build()
        imageAnalyzer = ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build()
            .also { it.setAnalyzer(backgroundExecutor) { image -> detectPose(image) } }
        cameraProvider.unbindAll(); try { camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer); preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) { Log.e(TAG, "Use case binding failed", exc) }
    }

    private fun detectPose(imageProxy: ImageProxy) { if (this::poseLandmarkerHelper.isInitialized && !isProcessingLlm) { poseLandmarkerHelper.detectLiveStream(imageProxy = imageProxy, isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT) } else { imageProxy.close() } }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) { val result = textToSpeech.setLanguage(Locale.US); if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) { Log.e(TAG, "The Language specified is not supported!") }
        } else { Log.e(TAG, "TTS Initialization Failed!") }
    }

    private fun speak(text: String) { textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "") }

    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation }

    override fun onError(error: String, errorCode: Int) { activity?.runOnUiThread { Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show() } }

    private fun getAngle(p1: NormalizedLandmark, p2: NormalizedLandmark, p3: NormalizedLandmark): Double {
        val angle = Math.toDegrees((atan2(p3.y() - p2.y(), p3.x() - p2.x()) - atan2(p1.y() - p2.y(), p1.x() - p2.x())).toDouble()); return if (angle < 0) angle + 360 else angle
    }

    private fun saveWorkoutSession(exercise: String, reps: Int, score: Int) {
        val dao = AppDatabase.getDatabase(requireContext()).workoutDao(); val session = WorkoutSession(date = Date(), exerciseType = exercise, reps = reps, score = score)
        lifecycleScope.launch(Dispatchers.IO) {
            dao.insert(session); val calendar = Calendar.getInstance(); val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            val challengeIndex = dayOfYear % dailyChallenges.size; val todayChallenge = dailyChallenges[challengeIndex]
            if (session.reps >= todayChallenge.repGoal && session.exerciseType.equals(todayChallenge.exerciseType, ignoreCase = true)) {
                val sharedPref = activity?.getSharedPreferences("WorkoutPrefs", Context.MODE_PRIVATE) ?: return@launch
                with(sharedPref.edit()) { putInt("LastChallengeCompletedDay", dayOfYear); apply() }
            }
        }
    }
}

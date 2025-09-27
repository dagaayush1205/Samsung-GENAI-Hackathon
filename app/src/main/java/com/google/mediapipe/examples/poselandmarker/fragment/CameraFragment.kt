package com.google.mediapipe.examples.poselandmarker.fragment

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import com.google.mediapipe.examples.poselandmarker.MainViewModel
import com.google.mediapipe.examples.poselandmarker.PoseLandmarkerHelper
import com.google.mediapipe.examples.poselandmarker.R
import com.google.mediapipe.examples.poselandmarker.data.AppDatabase
import com.google.mediapipe.examples.poselandmarker.data.WorkoutSession
import com.google.mediapipe.examples.poselandmarker.databinding.FragmentCameraBinding
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    private lateinit var heartRateReceiver: BroadcastReceiver
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
    private var lastFeedbackTime = 0L
    private var isGenerativeAiEnabled = false
    private var isAiThinking = false

    private val feedbackLibrary = mapOf(
        "HIPS_LOW" to "Lift your hips!",
        "HIPS_HIGH" to "Lower your hips!",
        "CHEST_UP" to "Keep your chest up!",
        "GOOD_FORM" to "Great form!"
    )

    override fun onResume() {
        super.onResume()
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(R.id.action_camera_to_permissions)
        }
        if (this::backgroundExecutor.isInitialized) {
            backgroundExecutor.execute {
                if (this::poseLandmarkerHelper.isInitialized) {
                    poseLandmarkerHelper.resume()
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
            backgroundExecutor.execute {
                poseLandmarkerHelper.clearPoseLandmarker()
            }
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(heartRateReceiver)
        if (this::backgroundExecutor.isInitialized) {
            backgroundExecutor.shutdown()
            backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        }
        if (this::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
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

        heartRateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val bpm = intent?.getIntExtra("BPM_EXTRA", 0) ?: 0
                if (bpm > 0) {
                    fragmentCameraBinding.heartRateText.text = "$bpm BPM"
                }
            }
        }
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            heartRateReceiver, IntentFilter("heart-rate-update")
        )

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

        fragmentCameraBinding.finishWorkoutButton.setOnClickListener {
            val exerciseName = if (currentWorkoutType == "SQUAT") "Squats" else "Push-ups"
            saveWorkoutSession(exerciseName, repCount, 100)
            activity?.finish()
        }

        fragmentCameraBinding.generativeAiButton.setOnClickListener {
            isGenerativeAiEnabled = !isGenerativeAiEnabled
            if (isGenerativeAiEnabled) {
                Toast.makeText(requireContext(), "Generative AI Coach Activated!", Toast.LENGTH_SHORT).show()
                fragmentCameraBinding.generativeAiButton.text = "Generative AI (ON)"
            } else {
                Toast.makeText(requireContext(), "Generative AI Coach Deactivated.", Toast.LENGTH_SHORT).show()
                fragmentCameraBinding.generativeAiButton.text = "Generative AI (OFF)"
            }
        }
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (_fragmentCameraBinding == null || isAiThinking) return@runOnUiThread

            val poseLandmarkerResult = resultBundle.results.first()
            fragmentCameraBinding.overlay.setResults(
                poseLandmarkerResult,
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                RunningMode.LIVE_STREAM
            )

            if (poseLandmarkerResult.landmarks().isNotEmpty()) {
                val feedbackMessage = analyzePose(poseLandmarkerResult)

                if (isGenerativeAiEnabled && feedbackMessage != "GOOD_FORM") {
                    triggerGenerativeFeedback(feedbackMessage)
                } else {
                    if (feedbackMessage == "GOOD_FORM") {
                        fragmentCameraBinding.aiFeedbackText.visibility = View.GONE
                    } else {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastFeedbackTime > 3000) { // 3-second cooldown
                            fragmentCameraBinding.aiFeedbackText.text = feedbackMessage
                            fragmentCameraBinding.aiFeedbackText.visibility = View.VISIBLE
                            speak(feedbackMessage)
                            lastFeedbackTime = currentTime
                        }
                    }
                }
            }
            fragmentCameraBinding.overlay.invalidate()
        }
    }

    private fun triggerGenerativeFeedback(feedbackType: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFeedbackTime < 5000) return

        isAiThinking = true
        lastFeedbackTime = currentTime

        lifecycleScope.launch(Dispatchers.Main) {
            poseLandmarkerHelper.pause()
            fragmentCameraBinding.aiThinkingIndicator.visibility = View.VISIBLE

            delay(1500)

            val generativeFeedback = when (feedbackType) {
                "HIPS_LOW" -> "Your hips are dropping. Engage your core to maintain a straight line from shoulders to heels."
                "HIPS_HIGH" -> "You're piking your hips. Try to lower them to align with your shoulders and ankles."
                "CHEST_UP" -> "You're leaning forward. Keep your chest proud and shoulders back as you descend."
                else -> "Focus on maintaining your form."
            }

            speak(generativeFeedback)
            fragmentCameraBinding.aiFeedbackText.text = generativeFeedback
            fragmentCameraBinding.aiFeedbackText.visibility = View.VISIBLE

            fragmentCameraBinding.aiThinkingIndicator.visibility = View.GONE
            poseLandmarkerHelper.resume()
            isAiThinking = false
        }
    }

    private fun analyzePose(poseLandmarkerResult: PoseLandmarkerResult): String {
        return when (currentWorkoutType) {
            "SQUAT" -> checkSquatFormAndCountReps(poseLandmarkerResult)
            else -> checkPushupFormAndCountReps(poseLandmarkerResult)
        }
    }

    private fun checkSquatFormAndCountReps(poseLandmarkerResult: PoseLandmarkerResult): String {
        val landmarks = poseLandmarkerResult.landmarks().first()
        val leftShoulder = landmarks[11]; val leftHip = landmarks[23]; val leftKnee = landmarks[25]; val leftAnkle = landmarks[27]

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
                // FIX: Removed .random()
                speak(feedbackLibrary["GOOD_FORM"]!!)
            }
        }

        if (workoutState == "down") {
            if (hipAngle < 150) {
                return feedbackLibrary["CHEST_UP"]!!
            }
        }
        
        return "GOOD_FORM"
    }

    private fun checkPushupFormAndCountReps(poseLandmarkerResult: PoseLandmarkerResult): String {
        val landmarks = poseLandmarkerResult.landmarks().first()
        val leftShoulder = landmarks[11]; val leftElbow = landmarks[13]; val leftWrist = landmarks[15]; val leftHip = landmarks[23]; val leftKnee = landmarks[25]

        val elbowAngle = getAngle(leftShoulder, leftElbow, leftWrist)
        val hipAngle = getAngle(leftShoulder, leftHip, leftKnee)

        if (elbowAngle < 90) {
            if (workoutState == "up") {
                workoutState = "down"
            }
        } else if (elbowAngle > 160) {
            if (workoutState == "down") {
                repCount++
                fragmentCameraBinding.repCountText.text = "Reps: $repCount"
                workoutState = "up"
                // FIX: Removed .random()
                speak(feedbackLibrary["GOOD_FORM"]!!)
            }
        }
        
        if (hipAngle < 150) {
            return feedbackLibrary["HIPS_LOW"]!!
        }
        if (hipAngle > 195) {
            return feedbackLibrary["HIPS_HIGH"]!!
        }

        return "GOOD_FORM"
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraFacing).build()
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation).build()
        imageAnalyzer = ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build()
            .also { it.setAnalyzer(backgroundExecutor) { image -> detectPose(image) } }
        cameraProvider.unbindAll()
        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectPose(imageProxy: ImageProxy) {
        if (this::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.detectLiveStream(
                imageProxy,
                isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
            )
        } else {
            imageProxy.close()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                Log.e(TAG, "The Language specified is not supported!")
            }
        } else {
            Log.e(TAG, "TTS Initialization Failed!")
        }
    }

    private fun speak(text: String) {
        if (this::textToSpeech.isInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread { Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show() }
    }

    private fun getAngle(
        p1: NormalizedLandmark, p2: NormalizedLandmark, p3: NormalizedLandmark
    ): Double {
        val angle = Math.toDegrees(
            (atan2(p3.y() - p2.y(), p3.x() - p2.x()) - atan2(
                p1.y() - p2.y(),
                p1.x() - p2.x()
            )).toDouble()
        )
        return if (angle < 0) angle + 360 else angle
    }

    private fun saveWorkoutSession(exercise: String, reps: Int, score: Int) {
        val dao = AppDatabase.getDatabase(requireContext()).workoutDao()
        lifecycleScope.launch(Dispatchers.IO) {
            dao.insert(WorkoutSession(date = Date(), exerciseType = exercise, reps = reps, score = score))
        }
    }
}

data class FormResult(val isCorrect: Boolean, val feedbackMessage: String)

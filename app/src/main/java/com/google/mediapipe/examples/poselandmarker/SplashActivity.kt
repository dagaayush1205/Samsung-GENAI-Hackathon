package com.google.mediapipe.examples.poselandmarker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    // Set your desired minimum display time in milliseconds. 2500L = 2.5 seconds.
    private val MINIMUM_DISPLAY_TIME = 2500L

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // We don't need a layout file for this modern approach
        // The theme handles the initial icon display.

        // Start a timer for the minimum display duration
        val startTime = System.currentTimeMillis()

        // Set the condition to keep the splash screen on.
        // It will stay on screen until the minimum time has passed.
        splashScreen.setKeepOnScreenCondition {
            val elapsedTime = System.currentTimeMillis() - startTime
            elapsedTime < MINIMUM_DISPLAY_TIME
        }
        
        // After the condition is met (minimum time passes), navigate to MainActivity.
        navigateToMain()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Finish this activity so the user can't navigate back to it.
    }
}

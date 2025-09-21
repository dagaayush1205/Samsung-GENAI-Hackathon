package poselandmarker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.mediapipe.examples.poselandmarker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startButton.setOnClickListener {
            // This is where you will launch the actual workout activity/camera screen
            // In the original example, this might be called 'MainActivity' as well,
            // so you might need to rename that one to 'WorkoutActivity'
            val intent = Intent(this, WorkoutActivity::class.java) // RENAME your old camera activity to WorkoutActivity
            startActivity(intent)
        }
    }
}


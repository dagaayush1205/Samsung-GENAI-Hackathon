package poselandmarker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.google.mediapipe.examples.poselandmarker.databinding.ActivityWorkoutBinding

class WorkoutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWorkoutBinding
    private val viewModel : MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        // If you want bottom nav, uncomment and add a BottomNavigationView in XML
        // binding.navigation.setupWithNavController(navController)
        // binding.navigation.setOnNavigationItemReselectedListener { }
    }

    override fun onBackPressed() {
        finish()
    }
}

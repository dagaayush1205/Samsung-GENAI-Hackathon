package com.example.samsunghackathon.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.samsunghackathon.presentation.theme.SamsungHackathonTheme
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.ValueKey
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.HealthTrackerCapability
import androidx.compose.foundation.layout.fillMaxWidth
import com.samsung.android.service.health.tracking.data.ValueKey.EcgSet
import androidx.compose.runtime.mutableStateListOf





class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var trackingService: HealthTrackingService? = null
    private var spo2Tracker: HealthTracker? = null
    private var stressTracker: HealthTracker? = null
    // Map of sensor type → live value
    private val sensorValues = mutableStateMapOf<Int, Float>()

    // List of sensors we want to observe
    private val sensorTypesToMonitor = listOf(
        Sensor.TYPE_HEART_RATE,
        Sensor.TYPE_STEP_COUNTER,
//        Sensor.TYPE_ACCELEROMETER,
//        Sensor.TYPE_GYROSCOPE
    )
    companion object {
        const val SENSOR_SPO2 = 1001 // arbitrary unique ID
        const val SENSOR_STRESS = 1002
    }

    private val healthTrackingService: HealthTrackingService? = null
    private val ecgTracker: HealthTracker? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Initialize values with 0
        sensorTypesToMonitor.forEach { sensorValues[it] = 0f }
        sensorValues[MainActivity.SENSOR_SPO2] = 0f
        sensorValues[MainActivity.SENSOR_STRESS] = 0f
        setupSpO2Tracker()
        setupStressTracker()
        //trackingService = HealthTrackingService(this)
        trackingService?.connectService()
        setContent {
            WearApp(sensorValues)
        }
    }

    override fun onResume() {
        super.onResume()
        sensorTypesToMonitor.forEach { type ->
            sensorManager.getDefaultSensor(type)?.also { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Can be used to track sensor accuracy changes
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type in sensorTypesToMonitor) {
                sensorValues[it.sensor.type] = it.values[0] // track first value
                Log.d("SensorUpdate", "${sensorTypeName(it.sensor.type)} = ${it.values[0]}")
            }
        }
    }


    private fun setupSpO2Tracker() {
            spo2Tracker = trackingService?.getHealthTracker(com.samsung.android.service.health.tracking.data.HealthTrackerType.SPO2_ON_DEMAND)

            spo2Tracker?.setEventListener(object: HealthTracker.TrackerEventListener{
                override fun onDataReceived(dataPoints: MutableList<DataPoint>) {
                    dataPoints?.forEach { dp ->
                        val spo2 = dp.getValue(com.samsung.android.service.health.tracking.data.ValueKey.SpO2Set.SPO2)
                        if (spo2 != null) {
                            sensorValues[SENSOR_SPO2] = spo2.toFloat()
                            Log.d("SamsungHealth", "SpO₂: $spo2 %")
                        }
                    }
                }

                override fun onFlushCompleted() {
                        TODO("Not yet implemented")
                }

                override fun onError(p0: HealthTracker.TrackerError?) {
                    TODO("Not yet implemented")

                }
            })
        }
    private fun setupStressTracker() {
        // Variable name updated for clarity
        val ppgTracker = trackingService?.getHealthTracker(com.samsung.android.service.health.tracking.data.HealthTrackerType.PPG_ON_DEMAND)

        ppgTracker?.setEventListener(object : HealthTracker.TrackerEventListener {
            override fun onDataReceived(dataPoints: MutableList<DataPoint>) {
                dataPoints.forEach { dp ->
                    // *** IMPORTANT: Use the correct ValueKey for PPG data ***
                    val ppgValue = dp.getValue(ValueKey.PpgSet.PPG_GREEN)

                    if (ppgValue != null) {
                        // Store it using your new constant
                        sensorValues[SENSOR_STRESS] = ppgValue.toFloat()
                        Log.d("SamsungHealth", "Raw PPG Green Value: $ppgValue")
                    }
                }
            }
            override fun onFlushCompleted() {}
            override fun onError(error: HealthTracker.TrackerError?) {
                Log.e("SamsungHealth", "PPG Tracker Error: $error")
            }
        })
    }


    fun onConnectionSuccess() {
        // This is the correct method for a successful connection.
        Log.d("HealthService", "ConnectionListener: onConnected!")
    }

    fun onDisconnected() {
        // This is called if the service disconnects unexpectedly.
        Log.w("HealthService", "ConnectionListener: onDisconnected!")
    }

//    fun onError(error: HealthTrackerException) {
//        // This is called if a connection-level error occurs.
//        Log.e("HealthService", "ConnectionListener: onError - $error")
//    }
    // Helper to map sensor type to a readable label
    private fun sensorTypeName(type: Int): String = when (type) {
        Sensor.TYPE_HEART_RATE -> "Heart Rate"
        Sensor.TYPE_STEP_COUNTER -> "Steps"
//        Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
//        Sensor.TYPE_GYROSCOPE -> "Gyroscope"
        MainActivity.SENSOR_SPO2 -> "SpO₂"
        MainActivity.SENSOR_STRESS -> "Stress Score"
        else -> "Unknown"
    }
}

@Composable
fun WearApp(sensorValues: Map<Int, Float>) {
    var hasPermission by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        val permission = Manifest.permission.BODY_SENSORS
        hasPermission =
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            permissionLauncher.launch(permission)
        }
    }

    SamsungHackathonTheme {
        Scaffold(timeText = { TimeText() }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background),
                contentAlignment = Alignment.Center
            ) {
                if (hasPermission) {
                    SensorsScreen(sensorValues)
                } else {
                    PermissionRequestScreen(
                        onGrantClick = {
                            permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SensorsScreen(sensorValues: Map<Int, Float>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        sensorValues.forEach { (type, value) ->
            SensorDisplay(type, value)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun SensorDisplay(sensorType: Int, value: Float) {
    val name = when (sensorType) {
        Sensor.TYPE_HEART_RATE -> "Heart Rate"
        Sensor.TYPE_STEP_COUNTER -> "Steps"
//        Sensor.TYPE_ACCELEROMETER -> "Accelerometer (X)"
//        Sensor.TYPE_GYROSCOPE -> "Gyroscope (X)"
        MainActivity.SENSOR_SPO2 -> "Spo2"
        MainActivity.SENSOR_STRESS -> "Stress Score"
         else -> "Unknown"
    }

//    if (sensorType == Sensor.TYPE_HEART_RATE) {
//        Icon(
//            imageVector = Icons.Default.Favorite,
//            contentDescription = "Heart Icon",
//            tint = Color(0xFFE57373),
//            modifier = Modifier.size(36.dp)
//        )
//    }
    if (sensorType == MainActivity.SENSOR_SPO2) {
        Icon(
            imageVector = Icons.Default.Favorite, // optional, or use custom SpO2 icon
            contentDescription = "SpO₂ Icon",
            tint = Color.Cyan,
            modifier = Modifier.size(36.dp)
        )
    }

    Text(
        text = "$name: ${if (value > 0) String.format("%.1f", value) else "--"}",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colors.primary,
    )
}

@Composable
fun PermissionRequestScreen(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Permission Required", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "This app needs access to body sensors to show live data.",
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onGrantClick) {
            Text("Grant Access")
        }
    }
}

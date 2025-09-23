package com.example.samsunghackathon.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.example.samsunghackathon.presentation.theme.SamsungHackathonTheme
import com.samsung.android.sdk.healthdata.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var healthDataStore: HealthDataStore? = null
    private val healthDataTypes = setOf(HealthConstants.HeartRate.HEALTH_DATA_TYPE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            healthDataStore = HealthDataStore(this, object : HealthDataStore.ConnectionListener {
                override fun onConnected() {
                    Log.d("HealthSDK", "HealthDataStore connected")
                }
                override fun onConnectionFailed(e: HealthConnectionErrorResult) {
                    Log.e("HealthSDK", "Connection failed: ${e.errorCode}")
                }
                override fun onDisconnected() {
                    Log.d("HealthSDK", "HealthDataStore disconnected")
                }
            })
            healthDataStore?.connectService()
        } catch (e: Exception) {
            Log.e("HealthSDK", "Initialization failed", e)
        }

        setContent {
            WearApp(healthDataStore, healthDataTypes)
        }
    }

    override fun onDestroy() {
        healthDataStore?.disconnectService()
        super.onDestroy()
    }
}

@Composable
fun WearApp(healthDataStore: HealthDataStore?, permissions: Set<String>) {
    var heartRate by remember { mutableStateOf(0.0) }
    var permissionsGranted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val permissionChecker by remember(healthDataStore) {
        mutableStateOf(HealthPermissionManager(healthDataStore, permissions))
    }

    // Check for permissions when the app launches
    LaunchedEffect(Unit) {
        try {
            val result = permissionChecker.isPermissionAcquired()
            permissionsGranted = result.all { it.value }
        } catch (e: Exception) {
            Log.e("HealthSDK", "Permission check failed", e)
        }
    }

    SamsungHackathonTheme {
        Scaffold(
            timeText = { TimeText() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background),
                contentAlignment = Alignment.Center
            ) {
                if (permissionsGranted) {
                    HeartRateScreen(
                        heartRate = heartRate,
                        isLoading = isLoading,
                        onRefreshClick = {
                            isLoading = true
                            readHeartRate(healthDataStore) { newHeartRate ->
                                heartRate = newHeartRate
                                // Simulate a network delay for better UX
                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(500)
                                    isLoading = false
                                }
                            }
                        }
                    )
                } else {
                    PermissionRequestScreen(
                        onGrantClick = {
                            try {
                                permissionChecker.requestPermissions().setResultListener { result ->
                                    permissionsGranted = result.all { it.value }
                                }
                            } catch (e: Exception) {
                                Log.e("HealthSDK", "Permission request failed", e)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HeartRateScreen(heartRate: Double, isLoading: Boolean, onRefreshClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = "Heart Icon",
            tint = Color(0xFFE57373), // A soft red color
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Heart Rate",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp))
        } else {
            Text(
                text = if (heartRate > 0) String.format("%.0f", heartRate) else "--",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
            )
        }

        Text(
            text = "BPM",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRefreshClick,
            enabled = !isLoading,
            modifier = Modifier.size(ButtonDefaults.LargeButtonSize)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        }
    }
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
        Text(
            text = "Permission Required",
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This app needs access to your heart rate data to function.",
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onGrantClick) {
            Text("Grant Access")
        }
    }
}


fun readHeartRate(store: HealthDataStore?, onResult: (Double) -> Unit) {
    if (store == null) {
        Log.e("HealthSDK", "HealthDataStore is not available")
        onResult(0.0)
        return
    }
    // Get the current time and the time 10 minutes ago
    val endTime = System.currentTimeMillis()
    val startTime = endTime - (1000 * 60 * 10)

    val request = HealthDataResolver.ReadRequest.Builder()
        .setDataType(HealthConstants.HeartRate.HEALTH_DATA_TYPE)
        .setLocalTimeRange(HealthConstants.HeartRate.START_TIME, startTime, HealthConstants.HeartRate.END_TIME, endTime)
        .setSortOrder(HealthConstants.HeartRate.END_TIME, HealthDataResolver.SortOrder.DESCENDING)
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val resolver = HealthDataResolver(store, null)
            resolver.read(request).setResultListener { result ->
                try {
                    val cursor = result.resultCursor
                    if (cursor != null && cursor.moveToFirst()) {
                        val heartRateValue = cursor.getFloat(cursor.getColumnIndex(HealthConstants.HeartRate.HEART_RATE))
                        onResult(heartRateValue.toDouble())
                    } else {
                        onResult(0.0) // No data found
                    }
                    cursor?.close()
                } finally {
                    result.release()
                }
            }
        } catch (e: Exception) {
            Log.e("HealthSDK", "Reading heart rate failed.", e)
            onResult(0.0)
        }
    }
}
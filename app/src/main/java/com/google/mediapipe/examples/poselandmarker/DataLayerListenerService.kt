package com.google.mediapipe.examples.poselandmarker

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class DataLayerListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d("DataLayerListener", "Received data change event")

        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                // Check if the data item's path is the one we're looking for
                if (event.dataItem.uri.path == "/heart_rate") {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val bpm = dataMap.getInt("BPM_KEY", 0)
                    Log.d("DataLayerListener", "Heart Rate from watch: $bpm")

                    // Broadcast the heart rate locally to our active CameraFragment
                    val intent = Intent("heart-rate-update")
                    intent.putExtra("BPM_EXTRA", bpm)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                }
            }
        }
    }
}

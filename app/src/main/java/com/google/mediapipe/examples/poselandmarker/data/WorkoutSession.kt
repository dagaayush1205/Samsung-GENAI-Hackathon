package com.google.mediapipe.examples.poselandmarker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "workout_history")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Date,
    val exerciseType: String,
    val reps: Int,
    val score: Int // A score out of 100 for form, etc.
)

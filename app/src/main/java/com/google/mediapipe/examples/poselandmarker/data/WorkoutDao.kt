package com.google.mediapipe.examples.poselandmarker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(session: WorkoutSession)

    @Query("SELECT * FROM workout_history ORDER BY date DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_history WHERE date >= :startDate AND date <= :endDate")
    fun getSessionsForWeek(startDate: Date, endDate: Date): Flow<List<WorkoutSession>>
}

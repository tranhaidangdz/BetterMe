package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.betterme.data.local.room.entities.HabitLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitLogDao {

    @Insert
    suspend fun insertLog(log: HabitLogEntity)

    @Update
    suspend fun updateLog(log: HabitLogEntity)

    @Delete
    suspend fun deleteLog(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE id = :logId")
    suspend fun deleteById(logId: Int)

    @Query("SELECT * FROM habit_logs WHERE habit_id = :habitId ORDER BY date DESC")
    fun getLogsByHabit(habitId: Int): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habit_id = :habitId AND date = :date")
    suspend fun getLogByDate(habitId: Int, date: Long): HabitLogEntity?

    // thống kê
    @Query("""
        SELECT COUNT(*) FROM habit_logs 
        WHERE habit_id = :habitId AND status = 'DONE'
    """)
    suspend fun countCompleted(habitId: Int): Int
}
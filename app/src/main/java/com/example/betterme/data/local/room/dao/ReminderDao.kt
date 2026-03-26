package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.betterme.data.local.room.entities.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert
    suspend fun insertReminder(reminder: ReminderEntity)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE habit_id = :habitId")
    suspend fun deleteByHabit(habitId: Int)

    @Query("SELECT * FROM reminders WHERE habit_id = :habitId")
    fun getRemindersByHabit(habitId: Int): Flow<List<ReminderEntity>>

    @Query("UPDATE reminders SET is_active = :isActive WHERE id = :id")
    suspend fun toggleReminder(id: Int, isActive: Boolean)
}
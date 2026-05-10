package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.betterme.data.local.room.entities.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE target_type = :type AND target_id = :id")
    fun observeForTarget(type: String, id: Int): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE target_type = :type AND target_id = :id AND is_active = 1 LIMIT 1")
    suspend fun getActiveByTarget(type: String, id: Int): ReminderEntity?

    @Query("DELETE FROM reminders WHERE target_type = :type AND target_id = :id")
    suspend fun deleteByTarget(type: String, id: Int)

    @Query("UPDATE reminders SET is_active = :isActive WHERE id = :id")
    suspend fun toggleReminder(id: Int, isActive: Boolean)

    @Query("UPDATE reminders SET work_id = :workId WHERE id = :id")
    suspend fun setWorkId(id: Int, workId: String?)

    // Backwards-compat helpers used by habit code that still thinks in terms of habit_id.
    @Query("SELECT * FROM reminders WHERE target_type = 'HABIT' AND target_id = :habitId")
    fun getRemindersByHabit(habitId: Int): Flow<List<ReminderEntity>>

    @Query("DELETE FROM reminders WHERE target_type = 'HABIT' AND target_id = :habitId")
    suspend fun deleteByHabit(habitId: Int)
}

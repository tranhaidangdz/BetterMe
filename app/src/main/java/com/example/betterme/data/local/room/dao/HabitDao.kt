package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.betterme.data.local.room.entities.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    // CREATE
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    // UPDATE
    @Update
    suspend fun updateHabit(habit: HabitEntity)

    // DELETE
    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: Int)

    // READ
    @Query("SELECT * FROM habits WHERE user_id = :userId")
    fun getHabitsByUser(userId: Int): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabitById(habitId: Int): HabitEntity?

    @Query("SELECT * FROM habits WHERE category_id = :categoryId")
    fun getHabitsByCategory(categoryId: Int): Flow<List<HabitEntity>>
}
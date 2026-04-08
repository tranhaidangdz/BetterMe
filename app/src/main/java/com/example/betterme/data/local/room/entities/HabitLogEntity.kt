package com.example.betterme.data.local.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habit_id")]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val habit_id: Int,
    val date: Long,
    val status: String,
    val note: String?,
    val image: String?,
    val created_at: Long
)
package com.example.betterme.data.local.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_chat")
data class AIChatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val user_id: String,
    val message: String,
    val response: String,
    val created_at: Long
)
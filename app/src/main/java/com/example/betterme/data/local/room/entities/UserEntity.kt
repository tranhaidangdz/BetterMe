package com.example.betterme.data.local.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val photoUrl: String,
    val created_at: Long,
    val coins: Int = 0,
    val level: Int = 1,
    val xp: Int = 0
)

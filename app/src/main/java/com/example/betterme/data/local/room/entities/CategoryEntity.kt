package com.example.betterme.data.local.room.entities

import androidx.room.PrimaryKey
import androidx.room.Entity

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val icon: Int
)

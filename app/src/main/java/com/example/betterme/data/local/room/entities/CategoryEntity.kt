package com.example.betterme.data.local.room.entities

import androidx.room.PrimaryKey
import androidx.room.Entity

/**
 * Global category catalog (Vận động, Học tập, Tinh thần, ...).
 *
 * Categories are shared across all users — per-user selection state lives in
 * [UserCategoryEntity], NOT here. The previous `isSelected: Boolean` field was
 * removed because it leaked selections across user accounts on the same device.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val icon: String,
    val description: String,
    /**
     * Optional Cloudinary (or any HTTPS) URL for category tile artwork. When set, the
     * Discover screen's CategoryTile prefers this over [icon] (emoji). Null = use emoji.
     */
    val image_url: String? = null
)

package com.example.betterme.data.local.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Per-user category selection.
 *
 * Categories themselves are global lookup data shared across all users; this table records
 * which categories a specific user has chosen during onboarding (or later edits). Replaces
 * the old global `CategoryEntity.isSelected` flag, which leaked one user's selections to the
 * next user that signed in on the same device.
 *
 * Carries `updated_at` / `synced_at` / `is_deleted` so [UserCategorySynchronizer] can
 * reconcile selections across devices via Firestore `users/{uid}/user_categories/{categoryId}`.
 */
@Entity(
    tableName = "user_categories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("user_id"),
        Index("category_id"),
        Index(value = ["user_id", "category_id"], unique = true)
    ]
)
data class UserCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val user_id: String,
    val category_id: Int,
    val created_at: Long = System.currentTimeMillis(),
    /** Bumped on insert / soft-delete so LWW can pick the latest selection state. */
    val updated_at: Long = created_at,
    /** Null until first successful push; row is dirty when `synced_at < updated_at` OR null. */
    val synced_at: Long? = null,
    /**
     * Soft-delete marker — deselecting a category sets this to true and bumps
     * `updated_at` so the removal replicates across devices.
     */
    val is_deleted: Boolean = false
)

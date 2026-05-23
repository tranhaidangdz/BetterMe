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
    val created_at: Long,
    /**
     * Wall-clock of last local mutation. Drives last-write-wins reconciliation
     * against the Firestore document.
     */
    val updated_at: Long = System.currentTimeMillis(),
    /** Last successful push timestamp. Dirty when synced_at < updated_at. */
    val synced_at: Long? = null,
    /** Soft-delete marker so user-initiated history wipes propagate to other devices. */
    val is_deleted: Boolean = false
)

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
    val xp: Int = 0,
    /**
     * Wall-clock of the last local mutation. Drives last-write-wins reconciliation
     * against the Firestore `users/{uid}` document — whichever side has the larger
     * `updated_at` wins. Stamped on every DAO write that mutates row state.
     */
    val updated_at: Long = System.currentTimeMillis(),
    /**
     * `updated_at` of the last successful push to Firestore. The row is "dirty"
     * (pending upload) whenever `synced_at < updated_at` or `synced_at == null`.
     */
    val synced_at: Long? = null
)

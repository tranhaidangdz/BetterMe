package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    suspend fun getUserById(id: String): UserEntity?

    fun observeUser(userId: String): Flow<UserEntity?>

    suspend fun insertUser(user: UserEntity)

    suspend fun updateUser(user: UserEntity)

    suspend fun deleteUser(user: UserEntity)

    suspend fun addCoins(userId: String, delta: Int)

    suspend fun setLevel(userId: String, level: Int)

    suspend fun recomputeLevel(userId: String)
}

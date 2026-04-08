package com.example.betterme.domain.repository

import com.example.betterme.data.local.room.entities.UserEntity

interface UserRepository {

    suspend fun getUserById(id: String): UserEntity?

    suspend fun insertUser(user: UserEntity)

    suspend fun updateUser(user: UserEntity)

    suspend fun deleteUser(user: UserEntity)
}

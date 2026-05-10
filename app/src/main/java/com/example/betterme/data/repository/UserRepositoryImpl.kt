package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.UserDao
import com.example.betterme.data.local.room.entities.UserEntity
import com.example.betterme.domain.repository.UserRepository

class UserRepositoryImpl(
    private val dao: UserDao
) : UserRepository {

    override suspend fun getUserById(id: String) = dao.getUserById(id)

    override fun observeUser(userId: String) = dao.observeUser(userId)

    override suspend fun insertUser(user: UserEntity) = dao.insertUser(user)

    override suspend fun updateUser(user: UserEntity) = dao.updateUser(user)

    override suspend fun deleteUser(user: UserEntity) = dao.deleteUser(user)

    override suspend fun addCoins(userId: String, delta: Int) = dao.addCoins(userId, delta)

    override suspend fun setLevel(userId: String, level: Int) = dao.setLevel(userId, level)

    override suspend fun recomputeLevel(userId: String) {
        val user = dao.getUserById(userId) ?: return
        val newLevel = 1 + (user.xp / 100)
        if (newLevel != user.level) {
            dao.setLevel(userId, newLevel)
        }
    }
}

package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.UserDao
import com.example.betterme.data.local.room.entities.UserEntity
import com.example.betterme.domain.repository.UserRepository

class UserRepositoryImpl(
    private val dao: UserDao
) : UserRepository {

    override suspend fun getUserById(id: String) = dao.getUserById(id)

    override suspend fun insertUser(user: UserEntity) = dao.insertUser(user)

    override suspend fun updateUser(user: UserEntity) = dao.updateUser(user)

    override suspend fun deleteUser(user: UserEntity) = dao.deleteUser(user)
}

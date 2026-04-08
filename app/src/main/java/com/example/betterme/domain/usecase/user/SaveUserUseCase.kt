package com.example.betterme.domain.usecase.user

import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.UserEntity
import com.example.betterme.domain.model.User
import com.example.betterme.domain.repository.UserRepository

class SaveUserUseCase(
    private val userRepository: UserRepository,
    private val dataStoreManager: DataStoreManager
) {
    suspend operator fun invoke(user: User): Result<Unit> {
        return try {
            val entity = UserEntity(
                id = user.id,
                name = user.name,
                email = user.email,
                photoUrl = user.photoUrl,
                rankId = user.rankId,
                created_at = System.currentTimeMillis()
            )
            userRepository.insertUser(entity)
            dataStoreManager.saveUserInfo(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

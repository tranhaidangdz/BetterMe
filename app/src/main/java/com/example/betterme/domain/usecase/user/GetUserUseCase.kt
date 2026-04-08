package com.example.betterme.domain.usecase.user

import com.example.betterme.domain.model.User
import com.example.betterme.domain.repository.UserRepository

class GetUserUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): Result<User?> {
        return try {
            val userEntity = userRepository.getUserById(userId)
            val user = userEntity?.let {
                User(
                    id = it.id,
                    name = it.name,
                    email = it.email,
                    photoUrl = it.photoUrl,
                    rankId = it.rankId
                )
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

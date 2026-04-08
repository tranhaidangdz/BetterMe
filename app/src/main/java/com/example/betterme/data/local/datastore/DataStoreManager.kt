package com.example.betterme.data.local.datastore

import com.example.betterme.domain.model.User
import kotlinx.coroutines.flow.Flow

interface DataStoreManager {
    fun isFirstTime(): Flow<Boolean>
    suspend fun setDoneFirstTime()
    suspend fun saveUserInfo(user: User)
    fun getUserInfo(): Flow<User?>
    suspend fun clearUserInfo()
}
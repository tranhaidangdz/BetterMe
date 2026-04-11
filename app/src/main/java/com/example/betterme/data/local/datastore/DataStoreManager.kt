package com.example.betterme.data.local.datastore

import com.example.betterme.domain.model.User
import kotlinx.coroutines.flow.Flow

interface DataStoreManager {
    fun isFirstTime(): Flow<Boolean>
    suspend fun setDoneFirstTime()
    suspend fun saveUserInfo(user: User)
    fun getUserInfo(): Flow<User?>
    suspend fun clearUserInfo()
    suspend fun saveGuestUser(guestId: String)
    fun isGuestUser(): Flow<Boolean>
    fun getCurrentUserId(): Flow<String?>
    suspend fun setHasSelectedHabits()
    fun hasSelectedHabits(): Flow<Boolean>
    suspend fun updateUserName(name: String)
    suspend fun updateUserPhotoUrl(photoUrl: String)
}
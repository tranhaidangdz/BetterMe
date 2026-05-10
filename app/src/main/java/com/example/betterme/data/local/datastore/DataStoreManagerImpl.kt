package com.example.betterme.data.local.datastore

import com.example.betterme.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit

class DataStoreManagerImpl(
    private val dataStore: DataStore<Preferences>
) : DataStoreManager {

    override fun isFirstTime(): Flow<Boolean> {
        return dataStore.data
            .map { preferences -> preferences[DataStoreKey.IS_FIRST_TIME] ?: true }
            .catch { exception ->
                emit(true)
            }
    }

    override suspend fun setDoneFirstTime() {
        dataStore.edit { preferences ->
            preferences[DataStoreKey.IS_FIRST_TIME] = false
        }
    }

    override suspend fun saveUserInfo(user: User) {
        dataStore.edit { preferences ->
            preferences[DataStoreKey.USER_ID] = user.id
            preferences[DataStoreKey.USER_NAME] = user.name
            preferences[DataStoreKey.USER_EMAIL] = user.email
            preferences[DataStoreKey.USER_PHOTO_URL] = user.photoUrl
        }
    }

    override fun getUserInfo(): Flow<User?> {
        return dataStore.data
            .map { preferences ->
                val id = preferences[DataStoreKey.USER_ID] ?: return@map null
                User(
                    id = id,
                    name = preferences[DataStoreKey.USER_NAME].orEmpty(),
                    email = preferences[DataStoreKey.USER_EMAIL].orEmpty(),
                    photoUrl = preferences[DataStoreKey.USER_PHOTO_URL].orEmpty()
                )
            }
            .catch {
                emit(null)
            }
    }

    override suspend fun clearUserInfo() {
        dataStore.edit { preferences ->
            preferences.remove(DataStoreKey.USER_ID)
            preferences.remove(DataStoreKey.USER_NAME)
            preferences.remove(DataStoreKey.USER_EMAIL)
            preferences.remove(DataStoreKey.USER_PHOTO_URL)
            preferences.remove(DataStoreKey.IS_GUEST)
            preferences.remove(DataStoreKey.HAS_SELECTED_HABITS)
        }
    }

    override suspend fun saveGuestUser(guestId: String) {
        dataStore.edit { preferences ->
            preferences[DataStoreKey.USER_ID] = guestId
            preferences[DataStoreKey.USER_NAME] = "Guest"
            preferences[DataStoreKey.USER_EMAIL] = ""
            preferences[DataStoreKey.USER_PHOTO_URL] = ""
            preferences[DataStoreKey.IS_GUEST] = true
        }
    }

    override fun isGuestUser(): Flow<Boolean> {
        return dataStore.data
            .map { preferences -> preferences[DataStoreKey.IS_GUEST] ?: false }
            .catch { emit(false) }
    }

    override fun getCurrentUserId(): Flow<String?> {
        return dataStore.data
            .map { preferences -> preferences[DataStoreKey.USER_ID] }
            .catch { emit(null) }
    }

    override suspend fun setHasSelectedHabits() {
        dataStore.edit { preferences ->
            preferences[DataStoreKey.HAS_SELECTED_HABITS] = true
        }
    }

    override fun hasSelectedHabits(): Flow<Boolean> {
        return dataStore.data
            .map { preferences -> preferences[DataStoreKey.HAS_SELECTED_HABITS] ?: false }
            .catch { emit(false) }
    }

    override suspend fun updateUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[DataStoreKey.USER_NAME] = name
        }
    }

    override suspend fun updateUserPhotoUrl(photoUrl: String) {
        dataStore.edit { preferences ->
            preferences[DataStoreKey.USER_PHOTO_URL] = photoUrl
        }
    }

    override suspend fun isChallengesSeeded(): Boolean {
        return try {
            dataStore.data.map { it[DataStoreKey.CHALLENGES_SEEDED] ?: false }.first()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun setChallengesSeeded() {
        dataStore.edit { preferences ->
            preferences[DataStoreKey.CHALLENGES_SEEDED] = true
        }
    }
}
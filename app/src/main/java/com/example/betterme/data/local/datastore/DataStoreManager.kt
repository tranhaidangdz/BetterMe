package com.example.betterme.data.local.datastore

import kotlinx.coroutines.flow.Flow

interface DataStoreManager {
    fun isFirstTime(): Flow<Boolean>
    suspend fun setDoneFirstTime()

}
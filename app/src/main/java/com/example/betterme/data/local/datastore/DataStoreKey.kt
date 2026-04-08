package com.example.betterme.data.local.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object DataStoreKey {
    val IS_FIRST_TIME = booleanPreferencesKey("is_first_time")
    val USER_ID = stringPreferencesKey("user_id")
    val USER_NAME = stringPreferencesKey("user_name")
    val USER_EMAIL = stringPreferencesKey("user_email")
    val USER_PHOTO_URL = stringPreferencesKey("user_photo_url")
    val USER_RANK_ID = stringPreferencesKey("user_rank_id")
}
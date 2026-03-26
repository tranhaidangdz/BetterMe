package com.example.betterme.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.datastore.DataStoreManagerImpl
import com.example.betterme.presentation.onboarding.OnboardingViewModel
import com.example.betterme.presentation.splash.SplashViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // DataStore
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create(
            produceFile = { get<Context>().preferencesDataStoreFile("betterme_preferences") }
        )
    }
    single<DataStoreManager> {
        DataStoreManagerImpl(get())
    }

    // Firebase
    single { FirebaseAuth.getInstance() }
    single {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()

        FirebaseFirestore.getInstance().apply {
            firestoreSettings = settings
        }
    }
}
val viewModelModule = module {
    viewModelOf(::SplashViewModel)
    viewModelOf(::OnboardingViewModel)
}
package com.example.betterme.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.datastore.DataStoreManagerImpl
import com.example.betterme.data.local.room.database.BetterMeDatabase
import com.example.betterme.data.repository.AIChatRepositoryImpl
import com.example.betterme.data.repository.AchievementRepositoryImpl
import com.example.betterme.data.repository.CategoryRepositoryImpl
import com.example.betterme.data.repository.ChallengeRepositoryImpl
import com.example.betterme.data.repository.HabitLogRepositoryImpl
import com.example.betterme.data.repository.HabitRepositoryImpl
import com.example.betterme.data.repository.ReminderRepositoryImpl
import com.example.betterme.data.repository.UserAchievementRepositoryImpl
import com.example.betterme.data.repository.UserChallengeRepositoryImpl
import com.example.betterme.domain.repository.AIChatRepository
import com.example.betterme.domain.repository.AchievementRepository
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.ChallengeRepository
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.repository.ReminderRepository
import com.example.betterme.domain.repository.UserAchievementRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.presentation.onboarding.habitselection.HabitSelectionViewModel
import com.example.betterme.presentation.onboarding.habitsuggestion.HabitSuggestionViewModel
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

val roomModule = module {
    // Database
    single {
        BetterMeDatabase.getDatabase(get())
    }
    // DAO
    single { get<BetterMeDatabase>().habitDao() }
    single { get<BetterMeDatabase>().habitLogDao() }
    single { get<BetterMeDatabase>().categoryDao() }
    single { get<BetterMeDatabase>().reminderDao() }
    single { get<BetterMeDatabase>().challengeDao() }
    single { get<BetterMeDatabase>().userChallengeDao() }
    single { get<BetterMeDatabase>().achievementDao() }
    single { get<BetterMeDatabase>().userAchievementDao() }
    single { get<BetterMeDatabase>().aiChatDao() }
}

val repositoryModule = module {

    single<HabitRepository> {
        HabitRepositoryImpl(get())
    }

    single<HabitLogRepository> {
        HabitLogRepositoryImpl(get())
    }

    single<ReminderRepository> {
        ReminderRepositoryImpl(get())
    }

    single<CategoryRepository> {
        CategoryRepositoryImpl(get())
    }

    single<ChallengeRepository> {
        ChallengeRepositoryImpl(get())
    }

    single<UserChallengeRepository> {
        UserChallengeRepositoryImpl(get())
    }

    single<AchievementRepository> {
        AchievementRepositoryImpl(get())
    }

    single<UserAchievementRepository> {
        UserAchievementRepositoryImpl(get())
    }

    single<AIChatRepository> {
        AIChatRepositoryImpl(get())
    }
}
val viewModelModule = module {
    viewModelOf(::SplashViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::HabitSelectionViewModel)
    viewModelOf(::HabitSuggestionViewModel)
}
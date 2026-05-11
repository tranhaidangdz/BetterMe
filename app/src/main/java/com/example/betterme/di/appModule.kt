package com.example.betterme.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.work.WorkManager
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.datastore.DataStoreManagerImpl
import com.example.betterme.data.local.room.database.BetterMeDatabase
import com.example.betterme.data.provider.GoogleAuthClient
import com.example.betterme.BuildConfig
import com.example.betterme.data.repository.AIChatRepositoryImpl
import com.example.betterme.data.repository.AchievementRepositoryImpl
import com.example.betterme.data.repository.CategoryRepositoryImpl
import com.example.betterme.data.repository.ChallengeLogRepositoryImpl
import com.example.betterme.data.repository.ChallengeRepositoryImpl
import com.example.betterme.data.repository.CloudinaryImageUploadRepositoryImpl
import com.example.betterme.data.repository.GroupTeamRepositoryImpl
import com.example.betterme.data.repository.HabitLogRepositoryImpl
import com.example.betterme.data.repository.HabitRepositoryImpl
import com.example.betterme.data.repository.LocalImageUploadRepositoryImpl
import com.example.betterme.data.repository.ReminderRepositoryImpl
import com.example.betterme.data.repository.NotificationRepositoryImpl
import com.example.betterme.data.repository.UserAchievementRepositoryImpl
import com.example.betterme.data.repository.UserCategoryRepositoryImpl
import com.example.betterme.data.repository.UserChallengeRepositoryImpl
import com.example.betterme.data.repository.UserRepositoryImpl
import com.example.betterme.domain.repository.AIChatRepository
import com.example.betterme.domain.repository.AchievementRepository
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.ChallengeLogRepository
import com.example.betterme.domain.repository.ChallengeRepository
import com.example.betterme.domain.repository.GroupTeamRepository
import com.example.betterme.domain.repository.HabitLogRepository
import com.example.betterme.domain.repository.HabitRepository
import com.example.betterme.domain.repository.ImageUploadRepository
import com.example.betterme.domain.repository.NotificationRepository
import com.example.betterme.domain.repository.ReminderRepository
import com.example.betterme.domain.repository.UserAchievementRepository
import com.example.betterme.domain.repository.UserCategoryRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.domain.repository.UserRepository
import com.example.betterme.domain.usecase.challenge.AwardChallengeCompletionUseCase
import com.example.betterme.domain.usecase.challenge.CancelChallengeReminderUseCase
import com.example.betterme.domain.usecase.challenge.ChallengeSeederUseCase
import com.example.betterme.domain.usecase.challenge.CheckInChallengeUseCase
import com.example.betterme.domain.usecase.challenge.JoinChallengeUseCase
import com.example.betterme.domain.usecase.challenge.LeaveChallengeUseCase
import com.example.betterme.domain.usecase.challenge.ScheduleChallengeReminderUseCase
import com.example.betterme.domain.usecase.challenge.ToggleStartReminderUseCase
import com.example.betterme.domain.usecase.habit.CancelHabitReminderUseCase
import com.example.betterme.domain.usecase.habit.RescheduleAllHabitRemindersUseCase
import com.example.betterme.domain.usecase.habit.ScheduleHabitReminderUseCase
import com.example.betterme.data.ai.AiHabitInsightRepositoryImpl
import com.example.betterme.data.ai.OpenRouterApi
import com.example.betterme.data.ai.OpenRouterNetwork
import com.example.betterme.data.repository.AiCacheRepositoryImpl
import com.example.betterme.domain.ai.AiCacheRepository
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.usecase.ai.GenerateHabitGroupReviewUseCase
import com.example.betterme.domain.usecase.ai.SuggestHabitsForCategoryUseCase
import com.example.betterme.domain.usecase.user.GetUserUseCase
import com.example.betterme.domain.usecase.user.SaveUserUseCase
import com.example.betterme.presentation.challenge.achievements.ChallengeAchievementsViewModel
import com.example.betterme.presentation.challenge.badges.ChallengeBadgesViewModel
import com.example.betterme.presentation.challenge.detail.ChallengeDetailViewModel
import com.example.betterme.presentation.challenge.discover.ChallengeDiscoverViewModel
import com.example.betterme.presentation.challenge.group.ChallengeGroupViewModel
import com.example.betterme.presentation.challenge.overview.ChallengeOverviewViewModel
import com.example.betterme.presentation.onboarding.habitselection.HabitSelectionViewModel
import com.example.betterme.presentation.onboarding.habitsuggestion.HabitSuggestionViewModel
import com.example.betterme.presentation.onboarding.OnboardingViewModel
import com.example.betterme.presentation.signin.SignInViewModel
import com.example.betterme.presentation.main.MainViewModel
import com.example.betterme.presentation.home.HomeViewModel
import com.example.betterme.presentation.dailyhabits.DailyHabitsViewModel
import com.example.betterme.presentation.addhabit.AddHabitViewModel
import com.example.betterme.presentation.categorydetail.CategoryDetailViewModel
import com.example.betterme.presentation.habitdetail.HabitDetailViewModel
import com.example.betterme.presentation.splash.SplashViewModel
import com.example.betterme.presentation.statistics.StatisticsViewModel
import com.example.betterme.utils.DeepLinkBus
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

    // Google Auth
    single { GoogleAuthClient(get()) }

    // WorkManager
    single { WorkManager.getInstance(get<Context>()) }

    // App-wide deep-link bus (notification taps → MainScreen routing)
    single { DeepLinkBus() }
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
    single { get<BetterMeDatabase>().userCategoryDao() }
    single { get<BetterMeDatabase>().reminderDao() }
    single { get<BetterMeDatabase>().challengeDao() }
    single { get<BetterMeDatabase>().userChallengeDao() }
    single { get<BetterMeDatabase>().challengeLogDao() }
    single { get<BetterMeDatabase>().groupTeamDao() }
    single { get<BetterMeDatabase>().achievementDao() }
    single { get<BetterMeDatabase>().userAchievementDao() }
    single { get<BetterMeDatabase>().notificationDao() }
    single { get<BetterMeDatabase>().aiChatDao() }
    single { get<BetterMeDatabase>().aiCacheDao() }
    single { get<BetterMeDatabase>().userDao() }
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

    single<UserCategoryRepository> {
        UserCategoryRepositoryImpl(get())
    }

    single<ChallengeRepository> {
        ChallengeRepositoryImpl(get())
    }

    single<UserChallengeRepository> {
        UserChallengeRepositoryImpl(get())
    }

    single<ChallengeLogRepository> {
        ChallengeLogRepositoryImpl(get())
    }

    single<GroupTeamRepository> {
        GroupTeamRepositoryImpl(get())
    }

    single<AchievementRepository> {
        AchievementRepositoryImpl(get())
    }

    single<UserAchievementRepository> {
        UserAchievementRepositoryImpl(get())
    }

    single<NotificationRepository> {
        NotificationRepositoryImpl(get())
    }

    single<AIChatRepository> {
        AIChatRepositoryImpl(get())
    }

    single<UserRepository> {
        UserRepositoryImpl(get())
    }

    // AI / OpenRouter — single Retrofit instance with a key-provider lambda so a
    // future settings screen can let users supply their own key without rebuilding
    // the network stack. When BuildConfig.OPENROUTER_API_KEY is blank the API call
    // will still go through but unauthenticated; OpenRouter returns 401 and the
    // repo surfaces a clear error message.
    single<OpenRouterApi> {
        OpenRouterNetwork.create(apiKeyProvider = { BuildConfig.OPENROUTER_API_KEY })
    }
    single<AiHabitInsightRepository> { AiHabitInsightRepositoryImpl(get()) }
    single<AiCacheRepository> { AiCacheRepositoryImpl(get()) }

    // Image upload repo: Cloudinary if configured, local-passthrough otherwise. Pick at
    // DI time so the rest of the app never has to branch on whether the cloud is set up.
    single<ImageUploadRepository> {
        val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
        val preset = BuildConfig.CLOUDINARY_UPLOAD_PRESET
        if (cloudName.isNotBlank() && preset.isNotBlank()) {
            CloudinaryImageUploadRepositoryImpl(uploadPreset = preset)
        } else {
            LocalImageUploadRepositoryImpl()
        }
    }
}

val useCaseModule = module {
    factory { GetUserUseCase(get()) }
    factory { SaveUserUseCase(get(), get()) }
    factory { ChallengeSeederUseCase(get(), get(), get(), get(), get()) }
    factory { JoinChallengeUseCase(get(), get(), get()) }
    factory { LeaveChallengeUseCase(get()) }
    factory {
        AwardChallengeCompletionUseCase(
            get(), get(), get(), get(), get(), get()
        )
    }
    factory {
        CheckInChallengeUseCase(
            get(), get(), get(), get(), get(), get()
        )
    }
    factory { ScheduleChallengeReminderUseCase(get(), get()) }
    factory { CancelChallengeReminderUseCase(get(), get()) }
    factory { ToggleStartReminderUseCase(get(), get(), get()) }

    // Habit reminders (AlarmManager exact alarms). Pulled out into use cases so
    // both the UI (AddHabit / HabitDetail) and the receivers (boot reschedule)
    // call the same scheduling code path — never duplicated.
    factory { ScheduleHabitReminderUseCase(get<Context>()) }
    factory { CancelHabitReminderUseCase(get<Context>()) }
    factory { RescheduleAllHabitRemindersUseCase(get(), get(), get(), get()) }

    // AI use cases — last `get()` is the AiCacheRepository.
    factory { GenerateHabitGroupReviewUseCase(get(), get(), get(), get(), get()) }
    factory { SuggestHabitsForCategoryUseCase(get(), get(), get(), get()) }
}

val viewModelModule = module {
    viewModelOf(::SplashViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::HabitSelectionViewModel)
    viewModelOf(::HabitSuggestionViewModel)
    viewModelOf(::SignInViewModel)
    viewModelOf(::MainViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::DailyHabitsViewModel)
    viewModelOf(::AddHabitViewModel)
    viewModelOf(::CategoryDetailViewModel)
    viewModelOf(::HabitDetailViewModel)
    viewModelOf(::StatisticsViewModel)
    viewModelOf(::ChallengeOverviewViewModel)
    viewModelOf(::ChallengeDetailViewModel)
    viewModelOf(::ChallengeDiscoverViewModel)
    viewModelOf(::ChallengeBadgesViewModel)
    viewModelOf(::ChallengeAchievementsViewModel)
    viewModelOf(::ChallengeGroupViewModel)
}
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
import com.example.betterme.data.repository.UserSettingsRepositoryImpl
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
import com.example.betterme.domain.repository.UserSettingsRepository
import com.example.betterme.domain.usecase.challenge.AwardChallengeCompletionUseCase
import com.example.betterme.domain.usecase.challenge.CancelChallengeReminderUseCase
import com.example.betterme.domain.usecase.challenge.ChallengeSeederUseCase
import com.example.betterme.domain.usecase.challenge.BackfillChallengeStatusesUseCase
import com.example.betterme.domain.usecase.challenge.CheckInChallengeUseCase
import com.example.betterme.domain.usecase.challenge.BuildChallengeProgressShareTextUseCase
import com.example.betterme.domain.usecase.challenge.EvaluateChallengeStatusUseCase
import com.example.betterme.domain.usecase.challenge.JoinChallengeUseCase
import com.example.betterme.domain.usecase.challenge.LeaveChallengeUseCase
import com.example.betterme.domain.usecase.challenge.ScheduleChallengeReminderUseCase
import com.example.betterme.domain.usecase.challenge.ToggleStartReminderUseCase
import com.example.betterme.domain.usecase.habit.CancelHabitReminderUseCase
import com.example.betterme.domain.usecase.habit.RescheduleAllHabitRemindersUseCase
import com.example.betterme.domain.usecase.habit.ScheduleHabitReminderUseCase
import com.example.betterme.data.ai.AiHabitInsightRepositoryImpl
import com.example.betterme.data.leaderboard.ChallengeLeaderboardRepositoryImpl
import com.example.betterme.data.leaderboard.FirebaseChallengeLeaderboardDataSource
import com.example.betterme.data.leaderboard.FirebaseGlobalLeaderboardDataSource
import com.example.betterme.data.leaderboard.GlobalCompetitorSeeder
import com.example.betterme.data.leaderboard.GlobalLeaderboardRepositoryImpl
import com.example.betterme.data.leaderboard.HybridCompetitorSeeder
import com.example.betterme.data.leaderboard.LeaderboardSessionMemory
import com.example.betterme.data.leaderboard.MotivationalEventEngine
import com.example.betterme.data.leaderboard.RankSnapshotStore
import com.example.betterme.data.repository.AiCacheRepositoryImpl
import com.example.betterme.data.share.ShareRepositoryImpl
import com.example.betterme.data.sync.AIChatSynchronizer
import com.example.betterme.data.sync.ChallengeLogSynchronizer
import com.example.betterme.data.sync.ConnectivityObserver
import com.example.betterme.data.sync.HabitLogSynchronizer
import com.example.betterme.data.sync.HabitSynchronizer
import com.example.betterme.data.sync.SyncCoordinator
import com.example.betterme.data.sync.SyncStatusRepository
import com.example.betterme.data.sync.UserAchievementSynchronizer
import com.example.betterme.data.sync.UserCategorySynchronizer
import com.example.betterme.data.sync.UserChallengeSynchronizer
import com.example.betterme.data.sync.UserProfileSynchronizer
import com.example.betterme.data.sync.UserSettingsSynchronizer
import com.example.betterme.domain.ai.AiCacheRepository
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.ai.AiHomeSessionMemory
import com.example.betterme.domain.repository.ChallengeLeaderboardRepository
import com.example.betterme.domain.repository.GlobalLeaderboardRepository
import com.example.betterme.domain.repository.ShareRepository
import com.example.betterme.domain.usecase.ai.AnalyzeHabitCreationUseCase
import com.example.betterme.domain.usecase.ai.AnalyzeHabitProgressionUseCase
import com.example.betterme.domain.usecase.ai.AnalyzeHabitRecoveryUseCase
import com.example.betterme.domain.usecase.ai.AnalyzeLifestyleUseCase
import com.example.betterme.domain.usecase.ai.AnalyzeScheduleUseCase
import com.example.betterme.domain.usecase.leaderboard.GetChallengeLeaderboardSummaryUseCase
import com.example.betterme.domain.usecase.leaderboard.GetChallengeLeaderboardUseCase
import com.example.betterme.domain.usecase.leaderboard.GetFriendLeaderboardUseCase
import com.example.betterme.domain.usecase.leaderboard.GetGlobalLeaderboardUseCase
import com.example.betterme.domain.usecase.leaderboard.GetLeaderboardProfileUseCase
import com.example.betterme.domain.usecase.leaderboard.GetMonthlyWinnersUseCase
import com.example.betterme.domain.usecase.leaderboard.SyncGlobalLeaderboardUseCase
import com.example.betterme.domain.usecase.leaderboard.SyncMyChallengeScoreUseCase
import com.example.betterme.domain.usecase.share.CreateShareUseCase
import com.example.betterme.domain.usecase.share.LoadSharedSnapshotUseCase
import com.example.betterme.domain.usecase.ai.ApplyScheduleSuggestionsUseCase
import com.example.betterme.domain.usecase.ai.GenerateHabitGroupReviewUseCase
import com.example.betterme.domain.usecase.ai.SuggestHabitsForCategoryUseCase
import com.example.betterme.domain.usecase.ai.SuggestOnboardingHabitsUseCase
import com.example.betterme.domain.usecase.user.GetUserUseCase
import com.example.betterme.domain.usecase.user.SaveUserUseCase
import com.example.betterme.presentation.challenge.achievements.ChallengeAchievementsViewModel
import com.example.betterme.presentation.challenge.badges.ChallengeBadgesViewModel
import com.example.betterme.presentation.challenge.detail.ChallengeDetailViewModel
import com.example.betterme.presentation.challenge.discover.ChallengeDiscoverViewModel
import com.example.betterme.presentation.challenge.group.ChallengeGroupViewModel
import com.example.betterme.presentation.challenge.overview.ChallengeOverviewViewModel
import com.example.betterme.presentation.sync.SyncStatusViewModel
import com.example.betterme.presentation.onboarding.habitselection.HabitSelectionViewModel
import com.example.betterme.presentation.onboarding.habitsuggestion.HabitSuggestionViewModel
import com.example.betterme.presentation.onboarding.OnboardingViewModel
import com.example.betterme.presentation.signin.SignInViewModel
import com.example.betterme.presentation.main.MainViewModel
import com.example.betterme.presentation.home.HomeViewModel
import com.example.betterme.presentation.addhabit.aicreation.HabitCreationAssistantViewModel
import com.example.betterme.presentation.home.progression.HabitProgressionAssistantViewModel
import com.example.betterme.presentation.home.recovery.HabitRecoveryAssistantViewModel
import com.example.betterme.presentation.leaderboard.LeaderboardViewModel
import com.example.betterme.presentation.leaderboard.global.GlobalLeaderboardViewModel
import com.example.betterme.presentation.share.profile.PublicProfileViewModel
import com.example.betterme.presentation.share.sheet.ShareProgressViewModel
import com.example.betterme.presentation.share.viewer.ShareViewerViewModel
import com.example.betterme.presentation.dailyhabits.DailyHabitsViewModel
import com.example.betterme.presentation.dailyhabits.schedule.ScheduleAnalysisViewModel
import com.example.betterme.presentation.onboarding.ai.OnboardingAiViewModel
import com.example.betterme.presentation.statistics.lifestyle.LifestyleInsightViewModel
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
        // Persistence ENABLED: Firestore's local SDK cache queues writes when
        // offline and replays them on reconnect, and serves reads from the cached
        // mirror so listeners stay responsive. This is the foundation of our
        // offline-first sync — the SDK owns the actual write queue; our sync
        // layer adds dirty-flag tracking, last-write-wins reconciliation across
        // restarts, retry-on-startup, and visible sync status.
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
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

    // ============================================================
    // Offline-first sync layer
    // ============================================================
    single { ConnectivityObserver(get()) }
    single { SyncStatusRepository(get()) }
    single { UserProfileSynchronizer(get(), get()) }
    single { UserChallengeSynchronizer(get(), get()) }
    single { ChallengeLogSynchronizer(get(), get(), get()) }
    single { AIChatSynchronizer(get(), get()) }
    single { UserSettingsSynchronizer(get(), get()) }
    single { HabitSynchronizer(get(), get()) }
    single { HabitLogSynchronizer(get(), get()) }
    single { UserCategorySynchronizer(get(), get()) }
    single { UserAchievementSynchronizer(get(), get()) }
    single {
        SyncCoordinator(
            dataStoreManager = get(),
            connectivity = get(),
            syncStatusRepository = get(),
            synchronizers = listOf(
                // Order: identity-shaping rows first (profile, settings, category
                // selections — these influence what the rest of the app shows),
                // then user-private content (habits → habit_logs, then user
                // challenges → challenge_logs), finally derived data (badges
                // depend on challenge completions; AI chat is independent but
                // bandwidth-heavy so it goes last to keep the critical-path
                // user-visible state landing first).
                get<UserProfileSynchronizer>(),
                get<UserSettingsSynchronizer>(),
                get<UserCategorySynchronizer>(),
                get<HabitSynchronizer>(),
                get<HabitLogSynchronizer>(),
                get<UserChallengeSynchronizer>(),
                get<ChallengeLogSynchronizer>(),
                get<UserAchievementSynchronizer>(),
                get<AIChatSynchronizer>()
            )
        )
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
    single { get<BetterMeDatabase>().userSettingsDao() }
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

    single<UserSettingsRepository> {
        UserSettingsRepositoryImpl(get())
    }

    // AI stack — Gemini only ----------------------------------------------
    //
    // GeminiApi ships as a Retrofit singleton with no baked-in key — the key
    // arrives per call from the rotating [ApiKeyPool]. Keys are sourced from
    // local.properties at build time and surface as a comma-separated list in
    // BuildConfig.GEMINI_API_KEYS; pool will be empty (and the router will
    // surface INVALID_KEY) when nothing is configured, which is the desired
    // "build but fail fast" behavior.
    single<com.example.betterme.data.ai.GeminiApi> { com.example.betterme.data.ai.GeminiNetwork.create() }

    // Single-provider key pool. Named-qualifier kept so a future second
    // provider can be added without resolver collisions.
    //
    // Startup diagnostic: emits one masked line to Logcat under `AiStartup` so
    // a developer can confirm BuildConfig.GEMINI_API_KEYS was actually populated
    // from local.properties at build time. Empty pool here is the #1 cause of
    // "AI never works" reports — and the log line is the fastest way to spot it
    // (`adb logcat -s AiStartup`).
    single(qualifier = org.koin.core.qualifier.named("geminiKeys")) {
        val raw = BuildConfig.GEMINI_API_KEYS.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val masked = raw.joinToString(", ") { com.example.betterme.data.ai.ApiKeyPool.mask(it) }
        android.util.Log.i(
            "AiStartup",
            "Gemini key pool: count=${raw.size} keys=[$masked] " +
                "buildConfig.len=${BuildConfig.GEMINI_API_KEYS.length}"
        )
        // Format-validate every key against the standard Google AI Studio shape
        // `AIza[A-Za-z0-9_-]{35}` (39 chars total, leading "AIza" prefix).  A
        // mismatch here means Gemini will 401/INVALID_KEY on the first call
        // using that slot — surface it now in the startup log so it's
        // diagnosable from `adb logcat -s AiStartup` without an actual request
        // being made.  The router still rotates past malformed keys at runtime,
        // but the warning saves the "why is my AI silent?" debug round-trip.
        val geminiKeyShape = Regex("^AIza[0-9A-Za-z_-]{35}$")
        raw.forEachIndexed { idx, key ->
            if (!geminiKeyShape.matches(key)) {
                android.util.Log.w(
                    "AiStartup",
                    "Gemini key #${idx + 1} (${com.example.betterme.data.ai.ApiKeyPool.mask(key)}, " +
                        "len=${key.length}) does NOT match the expected Google " +
                        "AI Studio format (AIza + 35 alphanum/_/- chars). " +
                        "Gemini will reject this key with 401. " +
                        "Replace it with one generated at https://aistudio.google.com/apikey."
                )
            }
        }
        if (raw.isEmpty()) {
            android.util.Log.w(
                "AiStartup",
                "No Gemini API keys configured. Add GEMINI_API_KEY=... (or " +
                    "GEMINI_API_KEYS=key1,key2) to local.properties and rebuild. " +
                    "AI features will surface AiErrorCategory.INVALID_KEY until then."
            )
        }
        com.example.betterme.data.ai.ApiKeyPool(keys = raw)
    }

    // Process-scoped per-provider health snapshot. Optional dependency of the
    // router — feeds Logcat-only diagnostics, never load-bearing.
    single { com.example.betterme.data.ai.AiProviderHealth() }

    // Router glues transports + pools. The repo only ever talks to this; it
    // never imports GeminiApi directly. Adding a second provider tomorrow is
    // a 1-file change to AiProvider + this map.
    single<com.example.betterme.data.ai.AiChatRouter> {
        com.example.betterme.data.ai.AiChatRouter(
            transports = mapOf(
                com.example.betterme.data.ai.AiProvider.GEMINI to
                    com.example.betterme.data.ai.GeminiChatTransport(get())
            ),
            pools = mapOf(
                com.example.betterme.data.ai.AiProvider.GEMINI to
                    get(qualifier = org.koin.core.qualifier.named("geminiKeys"))
            ),
            healthTracker = get()
        )
    }

    single { com.example.betterme.data.ai.SingleFlight() }
    single<AiHabitInsightRepository> { AiHabitInsightRepositoryImpl(get(), get()) }
    single<AiCacheRepository> { AiCacheRepositoryImpl(get()) }
    // Process-scoped throttle for Home AI surfaces — keeps Recovery /
    // Progression / Lifestyle cards from re-analyzing on every Home entry.
    single { AiHomeSessionMemory() }

    // Challenge leaderboard stack — Firestore datasource + deterministic
    // seeded competitors + session cache. The repository merges real
    // entries with seeded rivals at read time.
    single { FirebaseChallengeLeaderboardDataSource(get()) }
    single { HybridCompetitorSeeder() }
    single { LeaderboardSessionMemory() }
    // Phase 2 — persisted rank snapshots + local motivational engine.
    // RankSnapshotStore is backed by the same Preferences DataStore the
    // app already wires up so we don't allocate a new file handle.
    single { RankSnapshotStore(get()) }
    single { MotivationalEventEngine() }
    single<ChallengeLeaderboardRepository> {
        ChallengeLeaderboardRepositoryImpl(
            firestoreDs = get(),
            seeder = get(),
            sessionMemory = get(),
            dataStoreManager = get(),
            challengeRepository = get(),
            userChallengeRepository = get(),
            rankSnapshotStore = get(),
            motivationalEventEngine = get()
        )
    }

    // Phase 2B — Global / Friend / Winners stack. Shares the
    // RankSnapshotStore (namespaced) + LeaderboardSessionMemory with
    // the per-challenge stack so writes don't double-fire across the
    // 30s throttle window.
    single { FirebaseGlobalLeaderboardDataSource(get()) }
    single { GlobalCompetitorSeeder() }
    single<GlobalLeaderboardRepository> {
        GlobalLeaderboardRepositoryImpl(
            firestoreDs = get(),
            seeder = get(),
            sessionMemory = get(),
            dataStoreManager = get(),
            rankSnapshotStore = get()
        )
    }

    // Phase 4 — simple verified share. Direct Firestore SDK, no
    // Cloud Functions, no HMAC. Repo writes one doc per publish to
    // /shared_progress/{userId}; viewer reads from the same path.
    single<ShareRepository> { ShareRepositoryImpl(firestore = get(), auth = get()) }

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
    factory {
        com.example.betterme.domain.usecase.challenge.DemoDataSeederUseCase(
            get(), get(), get(), get(), get(), get(), get(), get()
        )
    }
    factory { JoinChallengeUseCase(get(), get(), get()) }
    factory { LeaveChallengeUseCase(get()) }
    factory {
        AwardChallengeCompletionUseCase(
            get(), get(), get(), get(), get(), get()
        )
    }
    factory {
        // userChallengeRepo, challengeRepo, challengeLogRepo, awardCompletionUseCase
        EvaluateChallengeStatusUseCase(get(), get(), get(), get())
    }
    factory {
        // database, challengeRepo, userChallengeRepo, challengeLogRepo,
        // groupTeamRepo, awardCompletionUseCase, evaluateStatusUseCase,
        // syncMyChallengeScore, syncGlobalLeaderboard
        CheckInChallengeUseCase(
            get(), get(), get(), get(), get(), get(), get(), get(), get()
        )
    }
    factory {
        BackfillChallengeStatusesUseCase(get(), get())
    }
    factory { ScheduleChallengeReminderUseCase(get(), get()) }
    factory { CancelChallengeReminderUseCase(get(), get()) }
    factory { ToggleStartReminderUseCase(get(), get(), get()) }
    factory { BuildChallengeProgressShareTextUseCase(get(), get(), get()) }

    // Habit reminders (AlarmManager exact alarms). Pulled out into use cases so
    // both the UI (AddHabit / HabitDetail) and the receivers (boot reschedule)
    // call the same scheduling code path — never duplicated.
    factory { ScheduleHabitReminderUseCase(get<Context>()) }
    factory { CancelHabitReminderUseCase(get<Context>()) }
    factory { RescheduleAllHabitRemindersUseCase(get(), get(), get(), get()) }

    // AI use cases — last `get()` is the AiCacheRepository.
    factory { GenerateHabitGroupReviewUseCase(get(), get(), get(), get(), get()) }
    // SuggestHabitsForCategoryUseCase — order: dataStore, habitRepo,
    // habitLogRepo, categoryRepo, aiRepo, cache.
    factory { SuggestHabitsForCategoryUseCase(get(), get(), get(), get(), get(), get()) }
    // Schedule Conflict Analyzer
    factory { AnalyzeScheduleUseCase(get(), get(), get(), get()) }
    factory { ApplyScheduleSuggestionsUseCase(get(), get(), get()) }
    // Onboarding AI Suggester — order: dataStore, aiRepo, cache, categoryRepo,
    // habitRepo, scheduleHabitReminderUseCase
    factory {
        SuggestOnboardingHabitsUseCase(get(), get(), get(), get(), get(), get())
    }
    // Adaptive Lifestyle Insight — order: dataStore, habitRepo, habitLogRepo,
    // aiRepo, cache
    factory { AnalyzeLifestyleUseCase(get(), get(), get(), get(), get()) }
    // Habit Creation Assistant — order: dataStore, habitRepo, habitLogRepo,
    // categoryRepo, aiRepo, cache
    factory { AnalyzeHabitCreationUseCase(get(), get(), get(), get(), get(), get()) }
    // Adaptive Habit Recovery — order: dataStore, habitRepo, habitLogRepo,
    // aiRepo, cache, scheduleHabitReminder
    factory { AnalyzeHabitRecoveryUseCase(get(), get(), get(), get(), get(), get()) }
    // Smart Habit Progression — order: dataStore, habitRepo, habitLogRepo,
    // aiRepo, cache
    factory { AnalyzeHabitProgressionUseCase(get(), get(), get(), get(), get()) }

    // Challenge leaderboard use cases.
    factory { GetChallengeLeaderboardUseCase(get()) }
    factory { GetChallengeLeaderboardSummaryUseCase(get()) }
    // SyncMyChallengeScore — order: dataStore, challengeRepo,
    // userChallengeRepo, challengeLogRepo, leaderboardRepository.
    factory { SyncMyChallengeScoreUseCase(get(), get(), get(), get(), get()) }

    // Phase 2B — Global leaderboard use cases.
    factory { GetGlobalLeaderboardUseCase(get()) }
    factory { GetFriendLeaderboardUseCase(get()) }
    factory { GetMonthlyWinnersUseCase(get()) }
    factory { GetLeaderboardProfileUseCase(get()) }
    // SyncGlobalLeaderboard — order: dataStore, habitRepo, habitLogRepo,
    // userChallengeRepo, challengeLogRepo, globalLeaderboardRepository.
    factory { SyncGlobalLeaderboardUseCase(get(), get(), get(), get(), get(), get()) }

    // Phase 4 — Verified Share use cases.
    // CreateShareUseCase order: dataStore, habitRepo, habitLogRepo,
    // userChallengeRepo, challengeLogRepo, shareRepository.
    factory { CreateShareUseCase(get(), get(), get(), get(), get(), get()) }
    factory { LoadSharedSnapshotUseCase(get()) }
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
    viewModelOf(::ScheduleAnalysisViewModel)
    viewModelOf(::OnboardingAiViewModel)
    viewModelOf(::LifestyleInsightViewModel)
    viewModelOf(::HabitCreationAssistantViewModel)
    viewModelOf(::HabitRecoveryAssistantViewModel)
    viewModelOf(::HabitProgressionAssistantViewModel)
    viewModelOf(::LeaderboardViewModel)
    viewModelOf(::GlobalLeaderboardViewModel)
    viewModelOf(::ShareProgressViewModel)
    viewModelOf(::ShareViewerViewModel)
    viewModelOf(::PublicProfileViewModel)
    viewModelOf(::AddHabitViewModel)
    viewModelOf(::CategoryDetailViewModel)
    viewModelOf(::HabitDetailViewModel)
    viewModelOf(::StatisticsViewModel)
    viewModelOf(::ChallengeOverviewViewModel)
    viewModelOf(::SyncStatusViewModel)
    viewModelOf(::ChallengeDetailViewModel)
    viewModelOf(::ChallengeDiscoverViewModel)
    viewModelOf(::ChallengeBadgesViewModel)
    viewModelOf(::ChallengeAchievementsViewModel)
    viewModelOf(::ChallengeGroupViewModel)
}
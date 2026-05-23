package com.example.betterme.di

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cloudinary.android.MediaManager
import com.example.betterme.BuildConfig
import com.example.betterme.data.receiver.HabitReminderReceiver
import com.example.betterme.data.sync.ConnectivityObserver
import com.example.betterme.data.worker.ChallengeReminderWorker
import com.example.betterme.data.worker.MidnightCleanupWorker
import com.example.betterme.data.worker.SyncWorker
import com.example.betterme.domain.usecase.challenge.BackfillChallengeStatusesUseCase
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.startKoin
import java.util.Calendar
import java.util.concurrent.TimeUnit

class KoinApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this@KoinApp)
        startKoin {
            androidContext(this@KoinApp)
            modules(
                appModule,
                viewModelModule,
                roomModule,
                repositoryModule,
                useCaseModule
            )
        }
        registerNotificationChannels()
        scheduleDailyMidnightCleanup()
        initCloudinary()
        runChallengeStatusBackfill()
        scheduleSync()
    }

    /**
     * Wire the offline-first sync layer into the app lifecycle:
     *
     *  1. Periodic SyncWorker — every 30 minutes, network-required. Survives process
     *     death; WorkManager handles backoff on failure.
     *  2. One-shot SyncWorker on launch — flushes anything dirty from the previous
     *     session (or marks rows as needing initial upload after the v12→v13 migration).
     *  3. Connectivity callback — when the device comes back online after being
     *     offline, enqueue a one-shot SyncWorker so queued writes flush within
     *     seconds instead of waiting up to 30 minutes for the next periodic tick.
     *
     * KEEP_existing policy on the periodic request means re-running KoinApp.onCreate
     * (e.g., process restart) doesn't reschedule duplicates.
     */
    private fun scheduleSync() {
        val wm = WorkManager.getInstance(this)
        wm.enqueueUniquePeriodicWork(
            SyncWorker.PERIODIC_UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            SyncWorker.periodicRequest()
        )
        wm.enqueueUniqueWork(
            SyncWorker.ONE_SHOT_UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            SyncWorker.oneShotRequest()
        )

        // Connectivity-resume trigger. drop(1) skips the initial emission so the
        // launch one-shot above isn't duplicated. filter { it } picks only the
        // online edge.
        val connectivity = GlobalContext.get().get<ConnectivityObserver>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        connectivity.observe()
            .drop(1)
            .distinctUntilChanged()
            .filter { online -> online }
            .onEach {
                wm.enqueueUniqueWork(
                    SyncWorker.ONE_SHOT_UNIQUE_NAME,
                    ExistingWorkPolicy.REPLACE,
                    SyncWorker.oneShotRequest()
                )
            }
            .launchIn(scope)
    }

    /**
     * Run the strict-daily evaluator across every ACTIVE / UPCOMING user_challenge row
     * exactly once per process launch. This catches the "user opens the app after several
     * days away" case — any row that crossed its deadline (or missed a day) gets its
     * permanent FAILED transition recorded before the UI flow loads it.
     *
     * Runs on a process-scoped IO supervisor so a single bad row can't tear down the
     * launch. Fire-and-forget: the UI does not block on this, but the first overview /
     * detail render that lands after backfill completes will see updated statuses
     * automatically because the Room flow re-emits on row updates.
     */
    private fun runChallengeStatusBackfill() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val useCase = GlobalContext.get().get<BackfillChallengeStatusesUseCase>()
                useCase()
            } catch (e: Exception) {
                Log.w("ChallengeBackfill", "Backfill scheduling failed", e)
            }
        }
    }

    /**
     * Initialize Cloudinary's MediaManager once per process so the SDK is ready by the
     * time the first upload fires. Quietly skips when no `cloud_name` is configured —
     * the DI module then picks the local-passthrough repository instead.
     */
    private fun initCloudinary() {
        val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
        val preset = BuildConfig.CLOUDINARY_UPLOAD_PRESET
        if (cloudName.isBlank() || preset.isBlank()) {
            Log.w(
                "CloudinaryInit",
                "Skipping init — cloudName='$cloudName' preset='$preset' " +
                    "(check local.properties + rebuild)"
            )
            return
        }
        try {
            MediaManager.init(this, mapOf("cloud_name" to cloudName))
            Log.i(
                "CloudinaryInit",
                "MediaManager.init OK cloudName=$cloudName preset=$preset"
            )
        } catch (e: IllegalStateException) {
            // MediaManager.init throws if it's already been initialized (e.g., process
            // restart in tests) — that's a no-op for our purposes.
            Log.d("CloudinaryInit", "Already initialized: ${e.message}")
        }
    }

    /**
     * Registers the alarm-style high-importance channel used by [ChallengeReminderWorker].
     * Channel sound + vibration + lights are enabled so reminders feel like an alarm,
     * not a passive notification. The channel can only be created once per install — we
     * intentionally use IMPORTANCE_HIGH from the start; users can downgrade in settings.
     */
    private fun registerNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Challenge reminders — alarm-style high-importance channel.
        val challengeChannel = NotificationChannel(
            ChallengeReminderWorker.CHANNEL_ID,
            ChallengeReminderWorker.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = ChallengeReminderWorker.CHANNEL_DESCRIPTION
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 150, 300)
            enableLights(true)
            setBypassDnd(false)
            setShowBadge(true)
            setSound(
                ChallengeReminderWorker.defaultAlarmSound(),
                ChallengeReminderWorker.alarmAudioAttributes()
            )
        }
        nm.createNotificationChannel(challengeChannel)

        // Habit reminders — dedicated channel so the user can independently tune
        // sound / vibration / importance vs. challenge reminders. Same high-priority
        // alarm-style defaults out of the box.
        val habitChannel = NotificationChannel(
            HabitReminderReceiver.CHANNEL_ID,
            HabitReminderReceiver.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = HabitReminderReceiver.CHANNEL_DESCRIPTION
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 100, 250)
            enableLights(true)
            setBypassDnd(false)
            setShowBadge(true)
            setSound(
                ChallengeReminderWorker.defaultAlarmSound(),
                ChallengeReminderWorker.alarmAudioAttributes()
            )
        }
        nm.createNotificationChannel(habitChannel)
    }

    /**
     * Schedules [MidnightCleanupWorker] to fire once per day at 00:00 local time. The
     * worker drops notifications older than 24h so the in-app inbox starts each day clean.
     */
    private fun scheduleDailyMidnightCleanup() {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelay = (target.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)

        val request = PeriodicWorkRequestBuilder<MidnightCleanupWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(MidnightCleanupWorker.UNIQUE_NAME)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            MidnightCleanupWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

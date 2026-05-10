package com.example.betterme.di

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.betterme.data.worker.ChallengeReminderWorker
import com.example.betterme.data.worker.MidnightCleanupWorker
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
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
        val channel = NotificationChannel(
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
        nm.createNotificationChannel(channel)
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

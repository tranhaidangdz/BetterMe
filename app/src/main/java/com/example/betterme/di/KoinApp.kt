package com.example.betterme.di

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.betterme.data.worker.ChallengeReminderWorker
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

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
    }

    private fun registerNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val challengeChannel = NotificationChannel(
            ChallengeReminderWorker.CHANNEL_ID,
            ChallengeReminderWorker.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = ChallengeReminderWorker.CHANNEL_DESCRIPTION
        }
        nm.createNotificationChannel(challengeChannel)
    }
}

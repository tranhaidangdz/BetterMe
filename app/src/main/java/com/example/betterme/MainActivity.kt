package com.example.betterme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.betterme.data.receiver.HabitReminderReceiver
import com.example.betterme.data.worker.ChallengeReminderWorker
import com.example.betterme.navigation.NavRoutes
import com.example.betterme.utils.DeepLinkBus
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val deepLinkBus: DeepLinkBus by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NavRoutes()
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return

        // betterme://share/{userId} OR betterme://profile/{userId} —
        // both forward into Main overlays. Handle first so notification
        // extras below don't mask them.
        if (intent.action == Intent.ACTION_VIEW) {
            val data = intent.data
            if (data?.scheme == "betterme") {
                val userId = data.pathSegments.firstOrNull().orEmpty()
                if (userId.isNotBlank()) {
                    when (data.host) {
                        "share" -> {
                            deepLinkBus.publish(DeepLinkBus.Event.OpenShareViewer(userId))
                            intent.data = null
                            return
                        }
                        "profile" -> {
                            deepLinkBus.publish(DeepLinkBus.Event.OpenPublicProfile(userId))
                            intent.data = null
                            return
                        }
                    }
                }
            }
        }

        val userChallengeId = intent.getIntExtra(
            ChallengeReminderWorker.EXTRA_OPEN_USER_CHALLENGE_ID, -1
        )
        if (userChallengeId > 0) {
            deepLinkBus.publish(DeepLinkBus.Event.OpenUserChallenge(userChallengeId))
            intent.removeExtra(ChallengeReminderWorker.EXTRA_OPEN_USER_CHALLENGE_ID)
            return
        }
        val challengeId = intent.getIntExtra(
            ChallengeReminderWorker.EXTRA_OPEN_CHALLENGE_ID, -1
        )
        if (challengeId > 0) {
            deepLinkBus.publish(DeepLinkBus.Event.OpenChallengePreview(challengeId))
            intent.removeExtra(ChallengeReminderWorker.EXTRA_OPEN_CHALLENGE_ID)
            return
        }
        // Habit reminder notification tap → open Habit Detail.
        val habitId = intent.getIntExtra(HabitReminderReceiver.EXTRA_OPEN_HABIT_ID, -1)
        if (habitId > 0) {
            deepLinkBus.publish(DeepLinkBus.Event.OpenHabitDetail(habitId))
            intent.removeExtra(HabitReminderReceiver.EXTRA_OPEN_HABIT_ID)
        }
    }
}

package com.example.betterme.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.betterme.domain.usecase.habit.RescheduleAllHabitRemindersUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Re-arms every habit reminder belonging to the current user.
 *
 * Triggered by:
 * - `ACTION_BOOT_COMPLETED`           — device reboot wiped all AlarmManager state.
 * - `ACTION_LOCKED_BOOT_COMPLETED`    — direct-boot variant.
 * - `ACTION_MY_PACKAGE_REPLACED`      — app upgrade reinstalled the process; alarms
 *                                       set against the prior install are gone.
 * - `ACTION_TIMEZONE_CHANGED`         — alarm "fire-at-millis" was set against a
 *                                       different tz; we need to recompute against
 *                                       the new local time.
 * - `ACTION_DATE_CHANGED`             — defensive: rare, but covers manual time edits.
 *
 * The actual rescheduling is delegated to [RescheduleAllHabitRemindersUseCase] which
 * queries the current user's habits from Room and re-arms each via the same path the
 * UI uses on create/edit, so there is one and only one scheduling code path.
 */
class HabitBootReceiver : BroadcastReceiver(), KoinComponent {

    private val rescheduleAll: RescheduleAllHabitRemindersUseCase by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return

        Log.i(TAG, "Rescheduling habit reminders after $action")
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleAll()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule habit reminders", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "HabitBootReceiver"

        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED
        )
    }
}

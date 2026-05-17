package com.example.betterme.presentation.share.utils

import android.content.Context
import android.content.Intent

/**
 * Wrapper around `Intent.ACTION_SEND` that routes the rich share text
 * through the OS chooser. Messenger / Zalo / Facebook / SMS / Email
 * all appear in the chooser automatically based on what's installed —
 * we don't enumerate target packages.
 *
 * Returning Boolean rather than launching unconditionally so the
 * caller can show a snackbar on the rare case where no app is
 * available (emulators without any social client installed).
 */
object ShareIntentHelper {

    fun shareText(context: Context, subject: String, text: String): Boolean {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, subject).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(chooser)
            true
        } catch (e: android.content.ActivityNotFoundException) {
            false
        }
    }
}

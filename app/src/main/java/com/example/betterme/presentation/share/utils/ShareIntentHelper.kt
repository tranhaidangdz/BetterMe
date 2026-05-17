package com.example.betterme.presentation.share.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Thin wrappers around `Intent.ACTION_SEND` for the share flow.
 *
 *  - [shareText] — plain text only. Used by the rich-message share.
 *  - [shareImage] — image attachment (+ optional caption text).
 *    Receiver app reads the URI via FileProvider's `content://` grant,
 *    so the caller MUST set FLAG_GRANT_READ_URI_PERMISSION on the
 *    initial intent. The chooser propagates that grant automatically.
 *
 * Both return Boolean so the caller can surface a "no chooser
 * available" snackbar on the rare emulator without any social client.
 */
object ShareIntentHelper {

    fun shareText(context: Context, subject: String, text: String): Boolean {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return launch(context, send, subject)
    }

    /**
     * Share a PNG (or any image) by content URI. The optional [caption]
     * is delivered via EXTRA_TEXT — Messenger / Zalo / Facebook pick
     * it up as the post body next to the attached image.
     */
    fun shareImage(
        context: Context,
        imageUri: Uri,
        subject: String,
        caption: String? = null
    ): Boolean {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            if (!caption.isNullOrBlank()) {
                putExtra(Intent.EXTRA_TEXT, caption)
            }
            // Critical — without this, the receiving app can't read the
            // FileProvider URI across the process boundary.
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return launch(context, send, subject)
    }

    private fun launch(context: Context, send: Intent, subject: String): Boolean {
        val chooser = Intent.createChooser(send, subject).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Forward the read-permission grant through the chooser so
            // any chosen target inherits it. createChooser does this
            // automatically on modern Android, but setting the flag
            // here is harmless on older OS versions.
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}

package com.example.betterme.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareUtils {

    /**
     * Open the system share sheet with a challenge-completion message and optional image.
     * The 4 cosmetic share buttons (FB / Zalo / IG / Khác) all funnel through this single
     * call — picking specific package names is fragile when apps aren't installed.
     */
    fun shareChallengeCompletion(
        context: Context,
        message: String,
        imageUri: Uri? = null,
        chooserTitle: String = "Chia sẻ thành tích"
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            if (imageUri != null) {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
            putExtra(Intent.EXTRA_TEXT, message)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

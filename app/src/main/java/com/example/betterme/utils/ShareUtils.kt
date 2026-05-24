package com.example.betterme.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.ArrayList

object ShareUtils {

    /**
     * Open the system share sheet with a challenge message + optional single image.
     * The cosmetic share buttons all funnel through this single call — picking
     * specific package names is fragile when apps aren't installed.
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

    /**
     * Open the system share sheet with a Vietnamese caption and a *gallery* of
     * check-in photos using `ACTION_SEND_MULTIPLE`. Each URI must come from the
     * app's FileProvider (already true for [ChallengeShareImagePrep] output).
     *
     * Behavior:
     *  - 0 images → falls through to text-only share (so a user with no photos
     *    yet still gets a working share button).
     *  - 1 image → uses regular ACTION_SEND so Instagram / Snapchat get the
     *    single-image flow they prefer.
     *  - ≥2 images → ACTION_SEND_MULTIPLE with `image/jpeg`; Messenger / Zalo /
     *    Facebook handle this natively and render a gallery preview.
     *
     * FLAG_GRANT_READ_URI_PERMISSION is set so the receiving app can read the
     * cached files without needing additional permissions.
     */
    fun shareChallengeWithImages(
        context: Context,
        message: String,
        imageUris: List<Uri>,
        chooserTitle: String = "Chia sẻ tiến độ"
    ) {
        when (imageUris.size) {
            0 -> shareChallengeCompletion(context, message, null, chooserTitle)
            1 -> shareChallengeCompletion(context, message, imageUris[0], chooserTitle)
            else -> {
                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "image/jpeg"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(imageUris))
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, chooserTitle).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        }
    }
}

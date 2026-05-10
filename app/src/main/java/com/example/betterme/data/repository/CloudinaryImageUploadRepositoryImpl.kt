package com.example.betterme.data.repository

import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.betterme.domain.repository.ImageUploadRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.MessageDigest
import kotlin.coroutines.resume

/**
 * Cloudinary-backed implementation of [ImageUploadRepository].
 *
 * - Uses the unsigned upload preset configured in `local.properties` so the API secret
 *   never ships with the app. The preset on Cloudinary should be marked "unsigned" and
 *   restricted to the folders we use (`challenge_checkins`, `habit_checkins`,
 *   `profile_photos`).
 * - Tags each upload with a stable hash of the local URI so retrying the same photo
 *   reuses the prior upload via Cloudinary's `public_id` deduplication, avoiding
 *   double-billing on bandwidth.
 * - Wraps Cloudinary's callback API into a suspend function via
 *   [suspendCancellableCoroutine] so call sites stay coroutine-friendly.
 *
 * If the upload fails for any reason, the local URI is returned as a fallback so the
 * check-in flow can still complete — the user still sees their photo locally even if
 * the cloud copy is missing.
 *
 * The actual `MediaManager.init(context, config)` call lives in [com.example.betterme.di.KoinApp]
 * so the SDK is initialized once per process.
 */
class CloudinaryImageUploadRepositoryImpl(
    private val uploadPreset: String
) : ImageUploadRepository {

    override suspend fun upload(
        localUri: Uri,
        folder: ImageUploadRepository.Folder
    ): String = suspendCancellableCoroutine { cont ->
        try {
            // Stable public_id derived from the URI string keeps re-uploads idempotent on
            // Cloudinary's side without needing client-side dedup state.
            val publicId = "${folder.path}/${stableHash(localUri.toString())}"

            val request = MediaManager.get()
                .upload(localUri)
                .unsigned(uploadPreset)
                .option("folder", folder.path)
                .option("public_id", publicId)
                .option("overwrite", false)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {}
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String
                            ?: resultData?.get("url") as? String
                            ?: localUri.toString()
                        if (cont.isActive) cont.resume(url)
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        Log.w(
                            TAG,
                            "Cloudinary upload failed: code=${error?.code} msg=${error?.description}"
                        )
                        if (cont.isActive) cont.resume(localUri.toString())
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                        // Cloudinary persisted the request to retry later — for our use case
                        // we don't want to block the check-in flow on retry, so resolve
                        // immediately with the local URI; a later upload will still register
                        // on Cloudinary's side under the same public_id.
                        if (cont.isActive) cont.resume(localUri.toString())
                    }
                })
                .dispatch()

            cont.invokeOnCancellation {
                runCatching { MediaManager.get().cancelRequest(request) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloudinary upload threw", e)
            if (cont.isActive) cont.resume(localUri.toString())
        }
    }

    private fun stableHash(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }

    private companion object {
        const val TAG = "CloudinaryUpload"
    }
}

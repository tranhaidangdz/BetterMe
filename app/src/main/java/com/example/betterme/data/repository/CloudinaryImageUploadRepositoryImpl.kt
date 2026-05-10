package com.example.betterme.data.repository

import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.betterme.domain.repository.ImageUploadRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Cloudinary-backed implementation of [ImageUploadRepository].
 *
 * Important contract rules for **unsigned** uploads — these are why the previous
 * version of this file caused `AndroidJobStrategy$UploadJob -> FAILED`:
 * - `overwrite` is **forbidden** at request time. Cloudinary returns 400 with
 *   "Overwrite parameter is not allowed when using unsigned upload". Overwrite
 *   policy lives on the preset, never on the request.
 * - `public_id` is **forbidden** unless the preset explicitly enables
 *   "Allow Public ID Override". The standard preset created via the Cloudinary
 *   console does NOT enable it, so passing a custom public_id triggers 400 too.
 * - `folder` IS allowed, so it's the only `.option(...)` we keep.
 *
 * With those two options removed, Cloudinary auto-generates a public_id within the
 * folder. We trade client-side dedup (the prior stable-hash public_id) for a request
 * the unsigned preset accepts. Coil's HTTP cache + Cloudinary's CDN handle the
 * caching layer for downloads, so per-photo uploads are still cheap.
 *
 * Logging is intentionally verbose: every start / success / failure prints the URI,
 * folder, request id and — on failure — the full ErrorInfo (code, description, http
 * status if exposed) plus the raw resultData on success. Look for the [TAG] tag in
 * logcat to follow the upload pipeline end-to-end.
 *
 * The actual `MediaManager.init(context, config)` call lives in
 * [com.example.betterme.di.KoinApp] so the SDK is initialized once per process.
 */
class CloudinaryImageUploadRepositoryImpl(
    private val uploadPreset: String
) : ImageUploadRepository {

    override suspend fun upload(
        localUri: Uri,
        folder: ImageUploadRepository.Folder
    ): String? = suspendCancellableCoroutine { cont ->
        Log.d(
            TAG,
            "upload[start] uri=$localUri folder=${folder.path} preset=$uploadPreset"
        )
        val mediaManager = try {
            MediaManager.get()
        } catch (e: IllegalStateException) {
            // MediaManager.get() throws if init hasn't run for this process. That's
            // a hard configuration failure — log loudly and surface null so the call
            // site falls through cleanly.
            Log.e(TAG, "upload[abort] MediaManager.get() failed — init never ran?", e)
            if (cont.isActive) cont.resume(null)
            return@suspendCancellableCoroutine
        }

        try {
            val request = mediaManager
                .upload(localUri)
                .unsigned(uploadPreset)
                // Folder is the ONLY option that's safe to pass on an unsigned request.
                // Do NOT add public_id or overwrite back here — they will 400.
                .option("folder", folder.path)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {
                        Log.d(TAG, "upload[onStart] requestId=$requestId")
                    }

                    override fun onProgress(
                        requestId: String?,
                        bytes: Long,
                        totalBytes: Long
                    ) {
                        if (totalBytes > 0) {
                            val pct = (bytes * 100 / totalBytes)
                            Log.v(TAG, "upload[onProgress] $requestId $pct% ($bytes/$totalBytes)")
                        }
                    }

                    override fun onSuccess(
                        requestId: String?,
                        resultData: MutableMap<Any?, Any?>?
                    ) {
                        val secureUrl = resultData?.get("secure_url") as? String
                        val urlFallback = resultData?.get("url") as? String
                        val publicId = resultData?.get("public_id") as? String
                        val resolved = secureUrl ?: urlFallback
                        Log.i(
                            TAG,
                            "upload[onSuccess] requestId=$requestId publicId=$publicId " +
                                "secureUrl=$secureUrl"
                        )
                        if (resolved == null) {
                            // Should never happen on a real Cloudinary 200 — log the entire
                            // payload so we can diagnose the schema if it does.
                            Log.w(TAG, "upload[onSuccess] no URL in payload: $resultData")
                        }
                        if (cont.isActive) cont.resume(resolved)
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        Log.e(
                            TAG,
                            "upload[onError] requestId=$requestId code=${error?.code} " +
                                "description=${error?.description}"
                        )
                        if (cont.isActive) cont.resume(null)
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                        // Cloudinary persisted the request for a later retry (typically a
                        // transient network error). We don't block the check-in flow on
                        // that retry — return null now; the queued retry will eventually
                        // land server-side under an auto-generated public_id.
                        Log.w(
                            TAG,
                            "upload[onReschedule] requestId=$requestId code=${error?.code} " +
                                "description=${error?.description}"
                        )
                        if (cont.isActive) cont.resume(null)
                    }
                })
                .dispatch()

            Log.d(TAG, "upload[dispatched] requestId=$request")

            cont.invokeOnCancellation {
                Log.d(TAG, "upload[cancelled] requestId=$request")
                runCatching { mediaManager.cancelRequest(request) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "upload[threw] uri=$localUri folder=${folder.path}", e)
            if (cont.isActive) cont.resume(null)
        }
    }

    private companion object {
        const val TAG = "CloudinaryUpload"
    }
}

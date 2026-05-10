package com.example.betterme.domain.repository

import android.net.Uri

/**
 * Single source of truth for "given a local image, return the URL we should persist".
 *
 * Implementations:
 * - `CloudinaryImageUploadRepositoryImpl` — uploads to Cloudinary and returns the secure
 *   delivery URL. Used in production once `CLOUDINARY_CLOUD_NAME` and
 *   `CLOUDINARY_UPLOAD_PRESET` are populated in `local.properties`.
 * - `LocalImageUploadRepositoryImpl` — passthrough that returns the original URI string.
 *   Selected at DI time when the Cloudinary keys are missing so the app still works
 *   offline / without cloud config.
 *
 * All call sites should go through this interface — never call Cloudinary's SDK
 * directly from view models or use cases.
 */
interface ImageUploadRepository {

    /** Folder hint used to organize uploads on the cloud. */
    enum class Folder(val path: String) {
        ChallengeCheckIn("challenge_checkins"),
        HabitCheckIn("habit_checkins"),
        Profile("profile_photos")
    }

    /**
     * Upload [localUri] and return the resolved URL that should be stored in the database.
     *
     * - On Cloudinary success: secure HTTPS URL.
     * - On Cloudinary failure or local fallback: the original [localUri] toString.
     *
     * Never throws — failures degrade to keeping the local URI so the app stays usable.
     */
    suspend fun upload(localUri: Uri, folder: Folder): String
}

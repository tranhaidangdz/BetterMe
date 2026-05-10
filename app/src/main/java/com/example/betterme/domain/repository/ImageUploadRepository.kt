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
     * Upload [localUri] and return the URL that should be persisted in the database.
     *
     * - Cloudinary impl: returns the `secure_url` on success, **null on failure** —
     *   callers must not persist transient local URIs that won't survive a cache rotation.
     * - Local-passthrough impl (no Cloudinary credentials configured): returns the
     *   original [localUri] toString — the device URI IS the only source of truth in
     *   that build, so it's safe to persist.
     *
     * Never throws.
     */
    suspend fun upload(localUri: Uri, folder: Folder): String?
}

package com.example.betterme.data.repository

import android.net.Uri
import com.example.betterme.domain.repository.ImageUploadRepository

/**
 * Fallback implementation of [ImageUploadRepository] that doesn't talk to any cloud
 * provider — it just returns the local URI as a string. Used when Cloudinary credentials
 * are not configured (e.g., open-source contributors building without secrets).
 */
class LocalImageUploadRepositoryImpl : ImageUploadRepository {
    override suspend fun upload(localUri: Uri, folder: ImageUploadRepository.Folder): String? =
        localUri.toString()
}

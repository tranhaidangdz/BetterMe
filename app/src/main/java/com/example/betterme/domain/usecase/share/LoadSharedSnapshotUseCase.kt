package com.example.betterme.domain.usecase.share

import com.example.betterme.domain.repository.ShareRepository

/**
 * Loads a published snapshot from Firestore for the share viewer
 * screen. Delegates to the repository; lives as a use case so the
 * VM doesn't import data-layer types directly.
 */
class LoadSharedSnapshotUseCase(
    private val repository: ShareRepository
) {
    suspend operator fun invoke(userId: String) = repository.loadByUserId(userId)
}

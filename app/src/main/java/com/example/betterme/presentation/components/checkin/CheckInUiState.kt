package com.example.betterme.presentation.components.checkin

import android.net.Uri

/**
 * Plain data carrier shared by habit + challenge check-in flows.
 * Decoupled from any feature-specific State so [CheckInConfirmSheet] can be reused.
 */
data class CheckInUiState(
    val photoUri: Uri? = null,
    val note: String = "",
    val timestamp: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val isSaving: Boolean = false
)

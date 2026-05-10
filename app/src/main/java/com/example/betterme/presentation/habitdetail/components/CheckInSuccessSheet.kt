package com.example.betterme.presentation.habitdetail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.betterme.presentation.components.checkin.CheckInSuccessSheet as SharedCheckInSuccessSheet

/**
 * Backwards-compat wrapper. New code should call the shared
 * [com.example.betterme.presentation.components.checkin.CheckInSuccessSheet] directly.
 */
@Composable
fun CheckInSuccessSheet(
    habitTitle: String,
    currentStreak: Int,
    onDismiss: () -> Unit,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    SharedCheckInSuccessSheet(
        entityTitle = habitTitle,
        currentStreak = currentStreak,
        onDismiss = onDismiss,
        onViewHistory = onViewHistory,
        modifier = modifier
    )
}

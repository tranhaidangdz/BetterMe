package com.example.betterme.presentation.habitdetail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.betterme.presentation.components.checkin.CheckInUiState
import com.example.betterme.presentation.habitdetail.HabitDetailState
import com.example.betterme.presentation.components.checkin.CheckInConfirmSheet as SharedCheckInConfirmSheet

/**
 * Backwards-compat wrapper. New code should call the shared
 * [com.example.betterme.presentation.components.checkin.CheckInConfirmSheet] directly.
 */
@Composable
fun CheckInConfirmSheet(
    state: HabitDetailState,
    onNoteChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    SharedCheckInConfirmSheet(
        state = CheckInUiState(
            photoUri = state.checkInPhotoUri,
            note = state.checkInNote,
            timestamp = state.checkInTimestamp,
            latitude = state.checkInLatitude,
            longitude = state.checkInLongitude,
            locationName = state.checkInLocationName,
            isSaving = state.isSavingCheckIn
        ),
        onNoteChanged = onNoteChanged,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier
    )
}

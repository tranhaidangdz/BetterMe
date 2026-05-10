package com.example.betterme.presentation.home

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.data.local.room.entities.NotificationEntity
import com.example.betterme.presentation.home.model.CantMiss
import com.example.betterme.presentation.home.model.HomeProgress

data class HomeState(
    val isLoading: Boolean = false,
    val userName: String = "",
    val userPhotoUrl: String = "",
    val progress: HomeProgress = HomeProgress(),
    val cantMissList: List<CantMiss> = emptyList(),
    val categoryGroups: List<HomeCategoryGroup> = emptyList(),
    val showEditProfileDialog: Boolean = false,
    // ----- Notification center -----
    val notifications: List<NotificationEntity> = emptyList(),
    val unreadNotificationCount: Int = 0,
    val showNotificationCenter: Boolean = false
) : MviViewState

data class HomeCategoryGroup(
    val categoryId: Int,
    val categoryName: String,
    val categoryIcon: String,
    val habitCount: Int,
    val color: androidx.compose.ui.graphics.Color
)

sealed class HomeIntent : MviIntent {
    data object LoadData : HomeIntent()
    data object ShowEditProfile : HomeIntent()
    data object DismissEditProfile : HomeIntent()
    data class UpdateUserName(val name: String) : HomeIntent()
    data class UpdateUserPhoto(val photoUri: String) : HomeIntent()
    data object Logout : HomeIntent()
    data object OpenNotificationCenter : HomeIntent()
    data object DismissNotificationCenter : HomeIntent()
    data class MarkNotificationRead(val id: Int) : HomeIntent()
    data object MarkAllNotificationsRead : HomeIntent()
}

sealed class HomeEvent : MviSingleEvent {
    data object NavigateToSignIn : HomeEvent()
    data class ShowError(val message: String) : HomeEvent()
    data class OpenChallengeDetail(val userChallengeId: Int) : HomeEvent()
    data class OpenChallengePreview(val challengeId: Int) : HomeEvent()
}

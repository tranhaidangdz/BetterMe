package com.example.betterme.presentation.challenge.group

import androidx.compose.ui.graphics.Color
import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.presentation.challenge.model.TeamUiModel

data class ChallengeGroupState(
    val isLoading: Boolean = true,
    val challengeId: Int = 0,
    val title: String = "",
    val accentColor: Color = Color(0xFF1A2540),
    val totalMembers: Int = 0,
    val daysCompleted: Int = 0,
    val totalDays: Int = 0,
    val progressPct: Int = 0,
    val teams: List<TeamUiModel> = emptyList(),
    val hasJoined: Boolean = false,
    val selectedTeamId: Int? = null,
    val showTeamPicker: Boolean = false
) : MviViewState

sealed class ChallengeGroupIntent : MviIntent {
    data class Load(val challengeId: Int) : ChallengeGroupIntent()
    data object OpenTeamPicker : ChallengeGroupIntent()
    data object DismissTeamPicker : ChallengeGroupIntent()
    data class JoinTeam(val teamId: Int) : ChallengeGroupIntent()
}

sealed class ChallengeGroupEvent : MviSingleEvent {
    data class Joined(val userChallengeId: Int) : ChallengeGroupEvent()
    data class ShowMessage(val text: String) : ChallengeGroupEvent()
}

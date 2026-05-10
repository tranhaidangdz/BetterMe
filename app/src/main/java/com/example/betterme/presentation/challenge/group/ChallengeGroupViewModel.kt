package com.example.betterme.presentation.challenge.group

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.GroupTeamEntity
import com.example.betterme.domain.repository.ChallengeRepository
import com.example.betterme.domain.repository.GroupTeamRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.domain.usecase.challenge.JoinChallengeUseCase
import com.example.betterme.presentation.challenge.model.TeamUiModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChallengeGroupViewModel(
    private val dataStoreManager: DataStoreManager,
    private val challengeRepository: ChallengeRepository,
    private val userChallengeRepository: UserChallengeRepository,
    private val groupTeamRepository: GroupTeamRepository,
    private val joinChallengeUseCase: JoinChallengeUseCase
) : BaseMviViewModel<ChallengeGroupIntent, ChallengeGroupState, ChallengeGroupEvent>() {

    override fun initState() = ChallengeGroupState()

    override fun processIntent(intent: ChallengeGroupIntent) {
        when (intent) {
            is ChallengeGroupIntent.Load -> load(intent.challengeId)
            ChallengeGroupIntent.OpenTeamPicker -> updateState { copy(showTeamPicker = true) }
            ChallengeGroupIntent.DismissTeamPicker -> updateState { copy(showTeamPicker = false) }
            is ChallengeGroupIntent.JoinTeam -> joinTeam(intent.teamId)
        }
    }

    private fun load(challengeId: Int) {
        viewModelScope.launch {
            updateState { copy(isLoading = true, challengeId = challengeId) }
            val challenge = challengeRepository.getById(challengeId)
            if (challenge == null) {
                updateState { copy(isLoading = false) }
                return@launch
            }
            val teams = groupTeamRepository.observeTeamsForChallenge(challengeId).first()
            val totalMembers = teams.sumOf { it.member_count }
            val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
            val joined = if (userId.isNotBlank()) {
                userChallengeRepository.getByUserAndChallenge(userId, challengeId)
            } else null
            updateState {
                copy(
                    isLoading = false,
                    title = challenge.title,
                    accentColor = parseColor(challenge.color_hex),
                    totalMembers = totalMembers,
                    daysCompleted = joined?.current_streak ?: 0,
                    totalDays = challenge.target_streak,
                    progressPct = joined?.progress_pct ?: 0,
                    teams = teams.map { it.toUi() },
                    hasJoined = joined != null && joined.status == "ACTIVE",
                    selectedTeamId = joined?.team_id
                )
            }
        }
    }

    private fun joinTeam(teamId: Int) {
        viewModelScope.launch {
            val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
            if (userId.isBlank()) {
                sendEvent(ChallengeGroupEvent.ShowMessage("Vui lòng đăng nhập"))
                return@launch
            }
            val ucId = joinChallengeUseCase(userId, currentState.challengeId, teamId)
            updateState {
                copy(
                    showTeamPicker = false,
                    hasJoined = true,
                    selectedTeamId = teamId
                )
            }
            sendEvent(ChallengeGroupEvent.Joined(ucId.toInt()))
            // Refresh
            load(currentState.challengeId)
        }
    }

    private fun GroupTeamEntity.toUi() = TeamUiModel(
        id = id,
        name = name,
        iconEmoji = icon_emoji,
        accentColor = parseColor(color_hex),
        memberCount = member_count,
        totalCoins = total_coins,
        rank = rank
    )

    private fun parseColor(hex: String): Color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF0077FF)
    }
}

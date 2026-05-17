package com.example.betterme.presentation.leaderboard.global

import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.domain.repository.GlobalLeaderboardRepository.FriendSnapshot
import com.example.betterme.domain.repository.GlobalLeaderboardRepository.GlobalSnapshot
import com.example.betterme.domain.leaderboard.MonthlyWinner
import com.example.betterme.domain.usecase.leaderboard.GetFriendLeaderboardUseCase
import com.example.betterme.domain.usecase.leaderboard.GetGlobalLeaderboardUseCase
import com.example.betterme.domain.usecase.leaderboard.GetLeaderboardProfileUseCase
import com.example.betterme.domain.usecase.leaderboard.GetMonthlyWinnersUseCase
import kotlinx.coroutines.launch

/**
 * VM for the tabbed global leaderboard surface. Per-tab cached
 * payloads sit on the state so a tab switch is instant once the data
 * has been loaded at least once.
 *
 * Re-entrancy: every load short-circuits while [isRefreshing] is true
 * so a tap-storm on the refresh pill or the tabs can't fan out into
 * parallel Firestore reads.
 */
class GlobalLeaderboardViewModel(
    private val getGlobal: GetGlobalLeaderboardUseCase,
    private val getFriends: GetFriendLeaderboardUseCase,
    private val getWinners: GetMonthlyWinnersUseCase,
    private val getProfile: GetLeaderboardProfileUseCase
) : BaseMviViewModel<GlobalLeaderboardIntent, GlobalLeaderboardState, GlobalLeaderboardEvent>() {

    override fun initState(): GlobalLeaderboardState = GlobalLeaderboardState()

    override fun processIntent(intent: GlobalLeaderboardIntent) {
        when (intent) {
            GlobalLeaderboardIntent.Initialize -> initialize()
            is GlobalLeaderboardIntent.SelectTab -> selectTab(intent.tab)
            GlobalLeaderboardIntent.Refresh -> refresh()
            is GlobalLeaderboardIntent.OpenProfile -> openProfile(intent.userId)
            GlobalLeaderboardIntent.CloseProfile -> updateState { copy(profileSheet = null) }
        }
    }

    private fun initialize() {
        if (currentState.ui is GlobalLeaderboardUi.Loaded) return
        viewModelScope.launch {
            updateState { copy(ui = GlobalLeaderboardUi.Loading) }
            loadActiveTab(forceRefresh = false)
        }
    }

    private fun selectTab(tab: GlobalLeaderboardTab) {
        if (tab == currentState.activeTab) return
        updateState { copy(activeTab = tab) }
        viewModelScope.launch { loadActiveTab(forceRefresh = false) }
    }

    private fun refresh() {
        if (currentState.isRefreshing) return
        updateState { copy(isRefreshing = true) }
        viewModelScope.launch { loadActiveTab(forceRefresh = true) }
    }

    /**
     * Load whichever tab is currently active. Keeps payloads for the
     * OTHER tabs in state so they don't blank when revisited.
     */
    private suspend fun loadActiveTab(forceRefresh: Boolean) {
        val state = currentState
        val previous = (state.ui as? GlobalLeaderboardUi.Loaded)
        var global: GlobalSnapshot? = previous?.global
        var friends: FriendSnapshot? = previous?.friends
        var winners: List<MonthlyWinner> = previous?.winners ?: emptyList()

        runCatching {
            when (state.activeTab) {
                GlobalLeaderboardTab.GLOBAL -> {
                    global = getGlobal(forceRefresh = forceRefresh)
                    // Best-effort prime the friends snapshot — friends
                    // pulls from the same merged pool so it's cheap.
                    if (friends == null) {
                        runCatching { friends = getFriends(forceRefresh = false) }
                    }
                }
                GlobalLeaderboardTab.FRIENDS -> {
                    friends = getFriends(forceRefresh = forceRefresh)
                }
                GlobalLeaderboardTab.WINNERS -> {
                    winners = getWinners()
                }
            }
        }.onFailure {
            updateState {
                copy(
                    ui = if (previous != null) previous
                    else GlobalLeaderboardUi.Error("Không thể tải bảng xếp hạng. Hãy thử lại."),
                    isRefreshing = false
                )
            }
            sendEvent(GlobalLeaderboardEvent.ShowError(it.message ?: "Network error"))
            return
        }

        updateState {
            copy(
                ui = GlobalLeaderboardUi.Loaded(
                    global = global,
                    friends = friends,
                    winners = winners
                ),
                isRefreshing = false
            )
        }
    }

    private fun openProfile(userId: String) {
        viewModelScope.launch {
            val profile = runCatching { getProfile(userId) }.getOrNull()
            updateState { copy(profileSheet = profile) }
        }
    }
}

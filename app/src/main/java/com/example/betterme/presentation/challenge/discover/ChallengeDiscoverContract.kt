package com.example.betterme.presentation.challenge.discover

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.presentation.challenge.model.CategoryTileUi
import com.example.betterme.presentation.challenge.model.FeaturedChallengeUiModel
import com.example.betterme.presentation.challenge.model.NewChallengeUiModel
import com.example.betterme.presentation.challenge.model.UpcomingFeatureUiModel
import com.example.betterme.presentation.challenge.shared.Difficulty

data class ChallengeDiscoverState(
    val isLoading: Boolean = true,
    val query: String = "",
    val selectedCategoryId: Int? = null,
    val selectedDifficulty: Difficulty? = null,
    val featured: List<FeaturedChallengeUiModel> = emptyList(),
    val upcomingFeatured: List<UpcomingFeatureUiModel> = emptyList(),
    val categories: List<CategoryTileUi> = emptyList(),
    val newest: List<NewChallengeUiModel> = emptyList(),
    val searchResults: List<NewChallengeUiModel> = emptyList(),
    /**
     * Map from difficulty tier → top-6 challenges in that tier, sorted by
     * participant count desc (proxy for popularity). Drives the
     * "Theo độ khó" grouped section on the Discover screen. Rendered only
     * when no filter / no search is active, so it's a passive showcase
     * rather than competing with the existing flat "Mới" list.
     */
    val groupedByDifficulty: Map<Difficulty, List<NewChallengeUiModel>> = emptyMap()
) : MviViewState

sealed class ChallengeDiscoverIntent : MviIntent {
    data object Load : ChallengeDiscoverIntent()
    data class UpdateQuery(val q: String) : ChallengeDiscoverIntent()
    data class SelectCategory(val id: Int?) : ChallengeDiscoverIntent()
    data class SelectDifficulty(val difficulty: Difficulty?) : ChallengeDiscoverIntent()
}

sealed class ChallengeDiscoverEvent : MviSingleEvent

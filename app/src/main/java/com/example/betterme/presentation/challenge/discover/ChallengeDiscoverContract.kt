package com.example.betterme.presentation.challenge.discover

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.presentation.challenge.model.CategoryTileUi
import com.example.betterme.presentation.challenge.model.FeaturedChallengeUiModel
import com.example.betterme.presentation.challenge.model.NewChallengeUiModel
import com.example.betterme.presentation.challenge.shared.Difficulty

data class ChallengeDiscoverState(
    val isLoading: Boolean = true,
    val query: String = "",
    val selectedCategoryId: Int? = null,
    val selectedDifficulty: Difficulty? = null,
    val featured: List<FeaturedChallengeUiModel> = emptyList(),
    val categories: List<CategoryTileUi> = emptyList(),
    val newest: List<NewChallengeUiModel> = emptyList(),
    val searchResults: List<NewChallengeUiModel> = emptyList()
) : MviViewState

sealed class ChallengeDiscoverIntent : MviIntent {
    data object Load : ChallengeDiscoverIntent()
    data class UpdateQuery(val q: String) : ChallengeDiscoverIntent()
    data class SelectCategory(val id: Int?) : ChallengeDiscoverIntent()
    data class SelectDifficulty(val difficulty: Difficulty?) : ChallengeDiscoverIntent()
}

sealed class ChallengeDiscoverEvent : MviSingleEvent

package com.example.betterme.presentation.main

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.presentation.main.model.MainTab

data class MainState(
    val selectedTab: MainTab = MainTab.HOME,
    val homeRefreshVersion: Int = 0,
    val categoryDetailId: Int? = null,
    val categoryDetailName: String = "",
    val categoryDetailIcon: String = ""
) : MviViewState

sealed class MainIntent : MviIntent {
    data class SelectTab(val tab: MainTab) : MainIntent()
    data object HabitAdded : MainIntent()
    data class OpenCategoryDetail(
        val categoryId: Int,
        val categoryName: String,
        val categoryIcon: String
    ) : MainIntent()
    data object CloseCategoryDetail : MainIntent()
}

sealed class MainEvent : MviSingleEvent

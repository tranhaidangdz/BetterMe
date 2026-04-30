package com.example.betterme.presentation.main

import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.presentation.main.model.MainTab

class MainViewModel : BaseMviViewModel<MainIntent, MainState, MainEvent>() {

    override fun initState(): MainState = MainState()

    override fun processIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.SelectTab -> updateState {
                copy(
                    selectedTab = intent.tab,
                    homeRefreshVersion = if (intent.tab == MainTab.HOME) homeRefreshVersion + 1 else homeRefreshVersion,
                    // Tự đóng CategoryDetail overlay khi chuyển tab
                    categoryDetailId = null,
                    categoryDetailName = "",
                    categoryDetailIcon = ""
                )
            }
            MainIntent.HabitAdded -> updateState { copy(homeRefreshVersion = homeRefreshVersion + 1) }
            is MainIntent.OpenCategoryDetail -> {
                updateState {
                    copy(
                        categoryDetailId = intent.categoryId,
                        categoryDetailName = intent.categoryName,
                        categoryDetailIcon = intent.categoryIcon
                    )
                }
            }
            MainIntent.CloseCategoryDetail -> {
                updateState {
                    copy(
                        categoryDetailId = null,
                        categoryDetailName = "",
                        categoryDetailIcon = ""
                    )
                }
            }
        }
    }
}

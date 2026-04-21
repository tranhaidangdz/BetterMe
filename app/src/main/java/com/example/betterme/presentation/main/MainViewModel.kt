package com.example.betterme.presentation.main

import com.example.betterme.base.BaseMviViewModel

class MainViewModel : BaseMviViewModel<MainIntent, MainState, MainEvent>() {

    override fun initState(): MainState = MainState()

    override fun processIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.SelectTab -> updateState { copy(selectedTab = intent.tab) }
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

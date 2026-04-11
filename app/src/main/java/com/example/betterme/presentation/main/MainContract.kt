package com.example.betterme.presentation.main

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.presentation.main.model.MainTab

data class MainState(
    val selectedTab: MainTab = MainTab.HOME
) : MviViewState

sealed class MainIntent : MviIntent {
    data class SelectTab(val tab: MainTab) : MainIntent()
}

sealed class MainEvent : MviSingleEvent

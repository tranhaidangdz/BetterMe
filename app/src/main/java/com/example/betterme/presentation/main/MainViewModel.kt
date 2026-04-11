package com.example.betterme.presentation.main

import com.example.betterme.base.BaseMviViewModel

class MainViewModel : BaseMviViewModel<MainIntent, MainState, MainEvent>() {

    override fun initState(): MainState = MainState()

    override fun processIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.SelectTab -> updateState { copy(selectedTab = intent.tab) }
        }
    }
}

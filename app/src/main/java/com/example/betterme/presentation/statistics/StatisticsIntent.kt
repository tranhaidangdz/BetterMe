package com.example.betterme.presentation.statistics

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent

sealed class StatisticsIntent : MviIntent {
    data object LoadData : StatisticsIntent()
    data class SelectTab(val tab: StatisticsTab) : StatisticsIntent()
    data class ToggleSection(val section: ExpandedSection) : StatisticsIntent()
}

sealed class StatisticsEvent : MviSingleEvent {
    data class ShowError(val message: String) : StatisticsEvent()
}

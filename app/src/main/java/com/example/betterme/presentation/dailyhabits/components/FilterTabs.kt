package com.example.betterme.presentation.dailyhabits.components

import androidx.compose.runtime.Composable
import com.example.betterme.presentation.components.view.PillSegmentedTabs
import com.example.betterme.presentation.dailyhabits.DailyHabitFilter

@Composable
fun FilterTabs(
    selectedFilter: DailyHabitFilter,
    onSelect: (DailyHabitFilter) -> Unit
) {
    PillSegmentedTabs(
        items = DailyHabitFilter.entries,
        selected = selectedFilter,
        label = { it.label },
        onSelect = onSelect
    )
}

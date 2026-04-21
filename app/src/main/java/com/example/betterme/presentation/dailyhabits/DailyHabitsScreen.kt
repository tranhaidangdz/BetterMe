package com.example.betterme.presentation.dailyhabits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.dailyhabits.components.DailyHabitsTopBar
import com.example.betterme.presentation.dailyhabits.components.DateSelector
import com.example.betterme.presentation.dailyhabits.components.FilterTabs
import com.example.betterme.presentation.dailyhabits.components.HabitCard
import com.example.betterme.presentation.theme.BetterMeColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun DailyHabitsScreen(
    viewModel: DailyHabitsViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()

    DailyHabitsContent(
        state = state,
        onIntent = viewModel::processIntent
    )
}

@Composable
fun DailyHabitsContent(
    state: DailyHabitsState,
    onIntent: (DailyHabitsIntent) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
    ) {
        item {
            DailyHabitsTopBar()
        }

        item {
            DateSelector(
                dates = state.dates,
                selectedIndex = state.selectedDateIndex,
                onSelect = { onIntent(DailyHabitsIntent.SelectDate(it)) }
            )
        }

        item {
            FilterTabs(
                selectedFilter = state.selectedFilter,
                onSelect = { onIntent(DailyHabitsIntent.SelectFilter(it)) }
            )
        }

        items(
            items = state.visibleHabits,
            key = { it.id }
        ) { habit ->
            HabitCard(habit = habit)
        }
    }
}

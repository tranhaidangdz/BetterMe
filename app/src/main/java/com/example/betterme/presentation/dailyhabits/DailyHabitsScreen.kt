package com.example.betterme.presentation.dailyhabits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.betterme.R
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.dailyhabits.components.DateSelector
import com.example.betterme.presentation.dailyhabits.components.FilterTabs
import com.example.betterme.presentation.dailyhabits.components.HabitCard
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundSecondary)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 140.dp)
        ) {
            item(key = "topbar") {
                BetterMeTopBar(
                    leadingIconRes = R.drawable.ic_arrow_left,
                    title = "Thói quen",
                    onLeadingClick = { }
                )
            }

            item(key = "date_selector") {
                DateSelector(
                    dates = state.dates,
                    selectedIndex = state.selectedDateIndex,
                    onSelect = { onIntent(DailyHabitsIntent.SelectDate(it)) }
                )
            }

            item(key = "filter_tabs") {
                FilterTabs(
                    selectedFilter = state.selectedFilter,
                    onSelect = { onIntent(DailyHabitsIntent.SelectFilter(it)) }
                )
            }

            if (state.visibleHabits.isEmpty() && !state.isLoading) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (state.selectedFilter) {
                                DailyHabitFilter.ALL -> "Chưa có thói quen nào trong ngày này"
                                DailyHabitFilter.IN_PROGRESS -> "Tất cả thói quen đã hoàn thành 🎉"
                                DailyHabitFilter.DONE -> "Chưa hoàn thành thói quen nào"
                            },
                            style = BetterMeTypography.Body.Medium,
                            color = BetterMeColors.Text.TextTertiary
                        )
                    }
                }
            }

            items(
                items = state.visibleHabits,
                key = { it.id }
            ) { habit ->
                HabitCard(
                    habit = habit,
                    onToggleCompletion = {
                        onIntent(DailyHabitsIntent.ToggleHabitCompletion(habit.id))
                    }
                )
            }
        }

        // Loading overlay
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BetterMeColors.Black.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = BetterMeColors.Primary.Primary,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

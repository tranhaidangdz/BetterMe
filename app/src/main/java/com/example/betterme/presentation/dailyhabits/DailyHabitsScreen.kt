package com.example.betterme.presentation.dailyhabits

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.betterme.presentation.dailyhabits.components.TasksEmptyState
import com.example.betterme.presentation.dailyhabits.components.TasksHeroCard
import com.example.betterme.presentation.theme.BetterMeColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun DailyHabitsScreen(
    onBackClick: () -> Unit = {},
    onHabitClick: (Int) -> Unit = {},
    viewModel: DailyHabitsViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()

    DailyHabitsContent(
        state = state,
        onIntent = viewModel::processIntent,
        onBackClick = onBackClick,
        onHabitClick = onHabitClick
    )
}

@Composable
fun DailyHabitsContent(
    state: DailyHabitsState,
    onIntent: (DailyHabitsIntent) -> Unit,
    onBackClick: () -> Unit = {},
    onHabitClick: (Int) -> Unit = {}
) {
    // Hero counters derive from the unfiltered list for the selected date so the
    // ring stays anchored to "today's plan" rather than reflecting whichever
    // filter tab the user happens to be on. completed = checked-in on selected
    // date, total = active habits on selected date.
    val total = state.allHabits.size
    val completed = state.allHabits.count { it.isCheckedInToday }
    val selectedDate = state.dates.getOrNull(state.selectedDateIndex)
    // Today-only interaction guard. Past/future rows render read-only; the hero
    // copy + ring alpha shift to communicate it.
    val isSelectedDateToday = state.selectedDateIndex == state.todayIndex
    val dayOffset = state.selectedDateIndex - state.todayIndex

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
            // Shared app top bar — consistent with the rest of the app. Title is
            // "Nhiệm vụ" (this screen models the user's daily missions, not the
            // catalog of habits).
            item(key = "topbar") {
                BetterMeTopBar(
                    leadingIconRes = R.drawable.ic_arrow_left,
                    title = "Nhiệm vụ",
                    onLeadingClick = onBackClick
                )
            }

            // Hero card with progress ring + counters. Title/subtitle/copy shift
            // when the user selects a non-today date so the screen never lies
            // ("Hôm nay" when the user is actually viewing Hôm qua).
            item(key = "hero") {
                TasksHeroCard(
                    completedCount = completed,
                    totalCount = total,
                    selectedDate = selectedDate,
                    isToday = isSelectedDateToday,
                    dayOffset = dayOffset
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
                item(key = "empty_state") {
                    TasksEmptyState(filter = state.selectedFilter)
                }
            }

            items(
                items = state.visibleHabits,
                key = { it.id }
            ) { habit ->
                // Filter switches re-key the list; animateItem smooths the
                // re-flow between filter tabs so cards slide rather than jump.
                HabitCard(
                    habit = habit,
                    onCardClick = { onHabitClick(habit.id) },
                    // Past + future days are read-only — only today permits
                    // check-in. Opening detail still works on every day.
                    isInteractive = isSelectedDateToday,
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(220),
                        placementSpec = tween(220),
                        fadeOutSpec = tween(160)
                    )
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

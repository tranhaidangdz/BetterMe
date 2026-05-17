package com.example.betterme.presentation.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.statistics.components.*
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import com.example.betterme.utils.DateUtils
import org.koin.androidx.compose.koinViewModel

@Composable
fun StatisticsScreen(
    onBackClick: () -> Unit = {},
    onHabitClick: (Int) -> Unit = {},
    viewModel: StatisticsViewModel = koinViewModel(),
    lifestyleVm: com.example.betterme.presentation.statistics.lifestyle.LifestyleInsightViewModel =
        koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()
    val lifestyleState by lifestyleVm.viewState.collectAsState()
    var showRangePicker by remember { mutableStateOf(false) }

    // Auto-load the coach insight once per screen entry. The VM short-circuits
    // while Loading and the use case reads from the 24h cache when fresh, so a
    // tab toggle / back-and-forth doesn't fire repeat OpenRouter calls.
    LaunchedEffect(Unit) {
        lifestyleVm.processIntent(
            com.example.betterme.presentation.statistics.lifestyle.LifestyleInsightIntent.Analyze()
        )
    }

    StatisticsContent(
        state = state,
        lifestyleState = lifestyleState,
        onTabSelected = { tab ->
            // Tapping "Tùy chọn" in the tab row opens the picker instead of dropping
            // the user on an empty CUSTOM tab with no range set. Every other tab
            // dispatches as normal.
            if (tab == StatisticsTab.CUSTOM) showRangePicker = true
            else viewModel.processIntent(StatisticsIntent.SelectTab(tab))
        },
        onToggleSection = { viewModel.processIntent(StatisticsIntent.ToggleSection(it)) },
        onBackClick = onBackClick,
        onOpenRangePicker = { showRangePicker = true },
        onResetCustomRange = {
            viewModel.processIntent(StatisticsIntent.SelectTab(StatisticsTab.WEEKLY))
        },
        onHabitClick = onHabitClick,
        onRefreshLifestyleInsight = {
            lifestyleVm.processIntent(
                com.example.betterme.presentation.statistics.lifestyle.LifestyleInsightIntent.Analyze(
                    forceRefresh = true
                )
            )
        }
    )

    if (showRangePicker) {
        DateRangePickerSheet(
            initialStart = state.customRangeStart ?: state.effectiveRangeStart.takeIf { it > 0 },
            initialEnd = state.customRangeEnd ?: state.effectiveRangeEnd.takeIf { it > 0 },
            onDismiss = { showRangePicker = false },
            onConfirm = { startMs, endMs ->
                viewModel.processIntent(
                    StatisticsIntent.SelectCustomRange(
                        start = DateUtils.startOfDay(startMs),
                        end = DateUtils.startOfDay(endMs)
                    )
                )
                showRangePicker = false
            }
        )
    }
}

@Composable
fun StatisticsContent(
    state: StatisticsState,
    lifestyleState: com.example.betterme.presentation.statistics.lifestyle.LifestyleInsightState =
        com.example.betterme.presentation.statistics.lifestyle.LifestyleInsightState(),
    onTabSelected: (StatisticsTab) -> Unit = {},
    onToggleSection: (ExpandedSection) -> Unit = {},
    onBackClick: () -> Unit = {},
    onOpenRangePicker: () -> Unit = {},
    onResetCustomRange: () -> Unit = {},
    onHabitClick: (Int) -> Unit = {},
    onRefreshLifestyleInsight: () -> Unit = {}
) {
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F8FF))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ===== HEADER + TABS =====
            item(key = "header") {
                StatisticsHeader(
                    selectedTab = state.selectedTab,
                    onTabSelected = onTabSelected,
                    onBackClick = onBackClick
                )
            }

            // ===== RANGE SUMMARY CHIP =====
            // Shows the effective date range under the tab row. Tap to re-pick a range,
            // tap "Đặt lại" while in CUSTOM mode to revert to the prior tab default.
            item(key = "range_chip") {
                if (state.effectiveRangeStart > 0 && state.effectiveRangeEnd > 0) {
                    RangeSummaryChip(
                        selectedTab = state.selectedTab,
                        effectiveStart = state.effectiveRangeStart,
                        effectiveEnd = state.effectiveRangeEnd,
                        onClick = onOpenRangePicker,
                        onReset = onResetCustomRange
                    )
                }
            }

            // ===== SMART INSIGHTS =====
            // Derived motivational copy. Hides itself entirely when the analytics
            // inputs aren't strong enough to produce signal lines.
            item(key = "smart_insights") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(450, delayMillis = 50)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(450, delayMillis = 50)
                    )
                ) {
                    SmartInsightsCard(
                        overview = state.overview,
                        streaks = state.streakAnalytics,
                        insights = state.additionalInsights,
                        challengeStats = state.challengeStats
                    )
                }
            }

            // ===== AI LIFESTYLE COACH =====
            // Long-term adaptive insight card from the Lifestyle Insight Engine.
            // Reads 14 days of habit completion + detected patterns; renders an
            // inline coach card with scores, trend, and 1-4 sustainable
            // suggestions. Auto-loads on screen entry via the LaunchedEffect
            // wired in StatisticsScreen — no user gesture required.
            item(key = "lifestyle_insight") {
                AnimatedVisibility(
                    visible = showContent &&
                        lifestyleState.ui !is com.example.betterme.presentation.statistics.lifestyle.LifestyleInsightUi.Idle,
                    enter = fadeIn(tween(500, delayMillis = 100)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(500, delayMillis = 100)
                    )
                ) {
                    com.example.betterme.presentation.statistics.lifestyle.LifestyleInsightCard(
                        state = lifestyleState,
                        onRefresh = onRefreshLifestyleInsight
                    )
                }
            }

            // ===== OVERVIEW SECTION =====
            item(key = "overview") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(400)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(400)
                    )
                ) {
                    OverviewCards(stats = state.overview)
                }
            }

            // ===== PIE CHART SECTION =====
            item(key = "pie_chart") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500, delayMillis = 100)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(500, delayMillis = 100)
                    )
                ) {
                    PieChartCard(stats = state.overview)
                }
            }

            // ===== STREAK ANALYTICS SECTION =====
            item(key = "streaks") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500, delayMillis = 200)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(500, delayMillis = 200)
                    )
                ) {
                    StreakCard(streaks = state.streakAnalytics)
                }
            }

            // ===== FEATURED HABITS SECTION =====
            item(key = "featured_header") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500, delayMillis = 300))
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Thói quen nổi bật",
                                style = BetterMeTypography.Title.Medium.SemiBold,
                                color = BetterMeColors.Text.TextPrimary
                            )
                            Text(
                                text = "Xem tất cả →",
                                style = BetterMeTypography.Body.Small.Medium,
                                color = BetterMeColors.Primary.Primary
                            )
                        }
                    }
                }
            }

            itemsIndexed(
                items = state.featuredHabits,
                key = { index, habit -> "featured_$index" }
            ) { index, habit ->
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(400, delayMillis = 350 + index * 80)) + slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(400, delayMillis = 350 + index * 80)
                    )
                ) {
                    FeaturedHabitCard(
                        habit = habit,
                        index = index
                    )
                }
            }

            // ===== HABIT JOURNEY SECTION =====
            item(key = "journey") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500, delayMillis = 500)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(500, delayMillis = 500)
                    )
                ) {
                    HabitJourneyCard(journey = state.habitJourney)
                }
            }

            // ===== HABIT STATUS LISTS (Expandable) =====
            item(key = "habit_list_header") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500, delayMillis = 600))
                ) {
                    Text(
                        text = "Danh sách thói quen",
                        style = BetterMeTypography.Title.Medium.SemiBold,
                        color = BetterMeColors.Text.TextPrimary
                    )
                }
            }

            // Completed habits
            item(key = "completed_section") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(400, delayMillis = 650)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(400, delayMillis = 650)
                    )
                ) {
                    HabitStatusSection(
                        title = "Hoàn thành",
                        section = ExpandedSection.COMPLETED,
                        isExpanded = state.expandedSection == ExpandedSection.COMPLETED,
                        habits = state.completedHabits,
                        onToggle = { onToggleSection(ExpandedSection.COMPLETED) },
                        onHabitClick = onHabitClick
                    )
                }
            }

            // Failed habits
            item(key = "failed_section") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(400, delayMillis = 700)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(400, delayMillis = 700)
                    )
                ) {
                    HabitStatusSection(
                        title = "Thất bại",
                        section = ExpandedSection.FAILED,
                        isExpanded = state.expandedSection == ExpandedSection.FAILED,
                        habits = state.failedHabits,
                        onToggle = { onToggleSection(ExpandedSection.FAILED) },
                        onHabitClick = onHabitClick
                    )
                }
            }

            // Ongoing habits
            item(key = "ongoing_section") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(400, delayMillis = 750)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(400, delayMillis = 750)
                    )
                ) {
                    HabitStatusSection(
                        title = "Đang thực hiện",
                        section = ExpandedSection.ONGOING,
                        isExpanded = state.expandedSection == ExpandedSection.ONGOING,
                        habits = state.ongoingHabits,
                        onToggle = { onToggleSection(ExpandedSection.ONGOING) },
                        onHabitClick = onHabitClick
                    )
                }
            }

            // ===== ADDITIONAL INSIGHTS =====
            item(key = "insights") {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500, delayMillis = 800)) + slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(500, delayMillis = 800)
                    )
                ) {
                    InsightCard(insights = state.additionalInsights)
                }
            }

            // Bottom spacer for navigation bar
            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(120.dp))
            }
        }

        // Loading overlay
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BetterMeColors.Black.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BetterMeColors.Primary.Primary)
            }
        }
    }
}

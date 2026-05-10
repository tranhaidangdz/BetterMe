package com.example.betterme.presentation.challenge.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.betterme.R
import com.example.betterme.presentation.challenge.badges.components.BadgeSection
import com.example.betterme.presentation.challenge.badges.components.BadgeStatusFilterRow
import com.example.betterme.presentation.challenge.badges.components.BadgeSummaryCard
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.theme.BetterMeColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChallengeBadgesScreen(
    onBackClick: () -> Unit,
    viewModel: ChallengeBadgesViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()

    val filteredSections = state.sections
        .map { section ->
            val filtered = when (state.filter) {
                BadgeStatusFilter.All -> section.badges
                BadgeStatusFilter.Earned -> section.badges.filter { it.isEarned }
                BadgeStatusFilter.Locked -> section.badges.filter { !it.isEarned }
            }
            section.copy(badges = filtered)
        }
        .filter { it.badges.isNotEmpty() }

    val lockedCount = state.totalBadges - state.earnedBadges

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundSecondary)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                BetterMeTopBar(
                    leadingIconRes = R.drawable.ic_arrow_left,
                    title = "Hệ thống huy hiệu",
                    onLeadingClick = onBackClick
                )
            }
            item {
                BadgeSummaryCard(
                    earned = state.earnedBadges,
                    total = state.totalBadges
                )
            }
            item {
                BadgeStatusFilterRow(
                    selected = state.filter,
                    earnedCount = state.earnedBadges,
                    lockedCount = lockedCount,
                    onSelect = { viewModel.processIntent(ChallengeBadgesIntent.SetFilter(it)) }
                )
            }
            items(filteredSections, key = { it.title }) { section ->
                BadgeSection(section = section)
            }
        }
    }
}

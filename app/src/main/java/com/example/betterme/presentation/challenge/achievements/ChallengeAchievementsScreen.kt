package com.example.betterme.presentation.challenge.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.betterme.R
import com.example.betterme.presentation.challenge.achievements.components.AchievementStatRow
import com.example.betterme.presentation.challenge.achievements.components.AchievementsHeaderCard
import com.example.betterme.presentation.challenge.achievements.components.BadgeCollectionRow
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.components.view.SectionHeader
import com.example.betterme.presentation.theme.BetterMeColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChallengeAchievementsScreen(
    onBackClick: () -> Unit,
    onSeeAllBadges: () -> Unit,
    viewModel: ChallengeAchievementsViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BetterMeTopBar(
                    leadingIconRes = R.drawable.ic_arrow_left,
                    title = "Thành tích của tôi",
                    onLeadingClick = onBackClick
                )
            }
            state.header?.let { header ->
                item {
                    AchievementsHeaderCard(
                        model = header,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            item {
                SectionHeader(
                    title = "Bộ sưu tập huy hiệu",
                    trailingLabel = "Xem tất cả →",
                    onTrailingClick = onSeeAllBadges,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item {
                BadgeCollectionRow(
                    badges = state.featuredBadges,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item {
                SectionHeader(
                    title = "Thành tích nổi bật",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            items(state.highlights, key = { it.label }) { hl ->
                AchievementStatRow(
                    model = hl,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

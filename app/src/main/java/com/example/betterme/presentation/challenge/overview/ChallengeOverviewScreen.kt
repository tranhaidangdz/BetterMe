package com.example.betterme.presentation.challenge.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.betterme.R
import com.example.betterme.presentation.challenge.model.OverviewFilter
import com.example.betterme.presentation.challenge.overview.components.ChallengeProgressCard
import com.example.betterme.presentation.challenge.overview.components.CompletedChallengeRow
import com.example.betterme.presentation.challenge.overview.components.OverviewHeroCard
import com.example.betterme.presentation.challenge.overview.components.UpcomingChallengeRow
import com.example.betterme.presentation.components.view.PillSegmentedTabs
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import com.example.betterme.utils.ShareUtils
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChallengeOverviewScreen(
    onOpenChallengeDetail: (Int) -> Unit = {},
    onOpenDiscover: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onOpenUpcoming: () -> Unit = {},
    onOpenCompleted: () -> Unit = {},
    onOpenGlobalLeaderboard: () -> Unit = {},
    viewModel: ChallengeOverviewViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.singleEvent.collectLatest { event ->
            when (event) {
                is ChallengeOverviewEvent.ShowMessage ->
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                is ChallengeOverviewEvent.LaunchShareSheet ->
                    ShareUtils.shareChallengeCompletion(
                        context = context,
                        message = event.text,
                        chooserTitle = "Chia sẻ tiến độ"
                    )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundSecondary)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OverviewTopBar(
                    onTrophyClick = onOpenAchievements,
                    onLeaderboardClick = onOpenGlobalLeaderboard,
                    onShareClick = {
                        viewModel.processIntent(ChallengeOverviewIntent.ShareProgress)
                    }
                )
            }

            item {
                OverviewHeroCard(
                    joined = state.stats.joined,
                    completed = state.stats.completed,
                    completionRate = state.stats.completionRate,
                    onTrophyClick = onOpenAchievements,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                PillSegmentedTabs(
                    items = OverviewFilter.entries,
                    selected = state.selectedFilter,
                    label = { it.label },
                    onSelect = { viewModel.processIntent(ChallengeOverviewIntent.SelectFilter(it)) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            when (state.selectedFilter) {
                OverviewFilter.Active -> {
                    if (state.active.isEmpty()) {
                        item { EmptyState("Chưa có thử thách nào đang diễn ra") }
                        item { ExploreCta(onClick = onOpenDiscover) }
                    } else {
                        items(state.active, key = { it.userChallengeId }) { active ->
                            ChallengeProgressCard(
                                model = active,
                                onContinue = onOpenChallengeDetail,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        item { ExploreCta(onClick = onOpenDiscover) }
                    }
                }
                OverviewFilter.Upcoming -> {
                    if (state.upcoming.isEmpty()) {
                        item { EmptyState("Chưa có thử thách sắp diễn ra") }
                    } else {
                        items(state.upcoming, key = { it.challengeId }) { row ->
                            UpcomingChallengeRow(
                                model = row,
                                onToggleReminder = { viewModel.processIntent(ChallengeOverviewIntent.ToggleStartReminder(it)) },
                                onClick = onOpenChallengeDetail,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Xem tất cả",
                                    style = BetterMeTypography.Body.Medium,
                                    color = BetterMeColors.Primary.Primary,
                                    modifier = Modifier.clickable { onOpenUpcoming() }
                                )
                            }
                        }
                    }
                }
                OverviewFilter.Completed -> {
                    if (state.completed.isEmpty()) {
                        item { EmptyState("Chưa có thử thách nào kết thúc") }
                    } else {
                        items(state.completed, key = { it.userChallengeId }) { row ->
                            CompletedChallengeRow(
                                model = row,
                                onClick = onOpenChallengeDetail,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Xem tất cả",
                                    style = BetterMeTypography.Body.Medium,
                                    color = BetterMeColors.Primary.Primary,
                                    modifier = Modifier.clickable { onOpenCompleted() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTopBar(
    onTrophyClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leaderboard icon on the left so the layout stays balanced
        // with the trophy on the right. Tapping opens the new tabbed
        // global leaderboard surface (Phase 2B).
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { onLeaderboardClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_medal),
                contentDescription = "Bảng xếp hạng",
                tint = BetterMeColors.Primary.Primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = "Thử thách",
            style = BetterMeTypography.Title.Medium.Bold,
            color = BetterMeColors.Text.TextPrimary,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        // Share progress → native ACTION_SEND chooser. Surfaces Messenger / Zalo /
        // Facebook / SMS based on what the user has installed.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { onShareClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_share),
                contentDescription = "Chia sẻ tiến độ",
                tint = BetterMeColors.Primary.Primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { onTrophyClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_trophy),
                contentDescription = "Thành tích",
                tint = BetterMeColors.Primary.Primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextTertiary
        )
    }
}

@Composable
private fun ExploreCta(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(BetterMeColors.Primary.PrimaryBackground)
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "+ Khám phá thử thách mới",
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Primary.Primary
            )
        }
    }
}

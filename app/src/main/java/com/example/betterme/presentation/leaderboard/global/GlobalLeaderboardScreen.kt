package com.example.betterme.presentation.leaderboard.global

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.betterme.R
import com.example.betterme.domain.leaderboard.GlobalLeaderboardEntry
import com.example.betterme.domain.leaderboard.MonthlyWinner
import com.example.betterme.domain.leaderboard.Season
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.leaderboard.components.LeaderboardSkeleton
import com.example.betterme.presentation.leaderboard.components.RankBadgeRow
import com.example.betterme.presentation.leaderboard.components.RankDeltaChip
import com.example.betterme.presentation.leaderboard.global.components.LeaderboardProfileSheet
import com.example.betterme.presentation.leaderboard.global.components.LeagueProgressBar
import com.example.betterme.presentation.leaderboard.global.components.LeagueTierBadge
import com.example.betterme.presentation.leaderboard.global.components.MonthlyWinnerCard
import com.example.betterme.presentation.leaderboard.global.components.RivalInsightCard
import com.example.betterme.presentation.leaderboard.global.components.SeasonCountdown
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography
import org.koin.androidx.compose.koinViewModel

/**
 * Cross-challenge leaderboard surface introduced in Phase 2B.
 *
 * Three-tab segmented control: Toàn cầu / Đối thủ / Quán quân
 * (Global / Friends / Monthly Winners). Each tab caches its payload
 * so flipping is instant once data is loaded.
 *
 * Polish layers shared with the per-challenge screen:
 *  - LeaderboardSkeleton on first load
 *  - RankBadgeRow + RankDeltaChip on every row
 *  - Animated score via animateIntAsState
 * Plus new components specific to this surface:
 *  - SeasonCountdown
 *  - LeagueTierBadge / LeagueProgressBar
 *  - LeaderboardProfileSheet on row tap
 *  - RivalInsightCard above the Friends list
 *  - MonthlyWinnerCard for the Winners tab
 */
@Composable
fun GlobalLeaderboardScreen(
    onBackClick: () -> Unit,
    viewModel: GlobalLeaderboardViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.processIntent(GlobalLeaderboardIntent.Initialize)
    }

    Scaffold(
        containerColor = BetterMeColors.BackGround.BackgroundSecondary
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .statusBarsPadding()
        ) {
            BetterMeTopBar(
                leadingIconRes = R.drawable.ic_arrow_left,
                title = "Bảng xếp hạng tháng",
                onLeadingClick = onBackClick
            )

            TabRow(
                active = state.activeTab,
                onSelect = { viewModel.processIntent(GlobalLeaderboardIntent.SelectTab(it)) }
            )

            when (val ui = state.ui) {
                GlobalLeaderboardUi.Loading -> LeaderboardSkeleton()
                is GlobalLeaderboardUi.Error -> ErrorState(
                    message = ui.message,
                    onRetry = { viewModel.processIntent(GlobalLeaderboardIntent.Refresh) }
                )
                is GlobalLeaderboardUi.Loaded -> LoadedBody(
                    state = state,
                    loaded = ui,
                    onRefresh = { viewModel.processIntent(GlobalLeaderboardIntent.Refresh) },
                    onOpenProfile = { uid ->
                        viewModel.processIntent(GlobalLeaderboardIntent.OpenProfile(uid))
                    }
                )
            }
        }
    }

    state.profileSheet?.let { profile ->
        LeaderboardProfileSheet(
            profile = profile,
            onDismiss = { viewModel.processIntent(GlobalLeaderboardIntent.CloseProfile) }
        )
    }
}

@Composable
private fun TabRow(
    active: GlobalLeaderboardTab,
    onSelect: (GlobalLeaderboardTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GlobalLeaderboardTab.entries.forEach { tab ->
            val selected = tab == active
            val bg = if (selected) BetterMeColors.Primary.Primary
            else BetterMeColors.Primary.Primary.copy(alpha = BetterMeTokens.AccentAlpha.Soft)
            val fg = if (selected) Color.White else BetterMeColors.Primary.Primary
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                    .background(bg)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = fg,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun LoadedBody(
    state: GlobalLeaderboardState,
    loaded: GlobalLeaderboardUi.Loaded,
    onRefresh: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    when (state.activeTab) {
        GlobalLeaderboardTab.GLOBAL -> GlobalTab(
            snapshot = loaded.global,
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            onOpenProfile = onOpenProfile
        )
        GlobalLeaderboardTab.FRIENDS -> FriendsTab(
            snapshot = loaded.friends,
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            onOpenProfile = onOpenProfile
        )
        GlobalLeaderboardTab.WINNERS -> WinnersTab(winners = loaded.winners)
    }
}

@Composable
private fun GlobalTab(
    snapshot: com.example.betterme.domain.repository.GlobalLeaderboardRepository.GlobalSnapshot?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    if (snapshot == null) {
        LeaderboardSkeleton()
        return
    }
    val mine = snapshot.myEntry
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            HeadlineRow(
                participantCount = snapshot.totalParticipants,
                seasonLabel = Season.displayLabel(Season.current()),
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
        }
        item { SeasonCountdown(daysLeft = snapshot.seasonEndsInDays) }
        if (mine != null) {
            item { LeagueProgressBar(progress = mine.leagueProgress) }
            // Sticky-style self card immediately under the progress bar
            // so the user always sees their own row even at low ranks.
            if (mine.rank > 5) {
                item {
                    GlobalRow(
                        entry = mine,
                        highlight = true,
                        onTap = { onOpenProfile(mine.userId) }
                    )
                }
            }
        }
        items(snapshot.entries, key = { it.userId }) { entry ->
            GlobalRow(
                entry = entry,
                highlight = entry.isCurrentUser,
                onTap = { onOpenProfile(entry.userId) }
            )
        }
    }
}

@Composable
private fun FriendsTab(
    snapshot: com.example.betterme.domain.repository.GlobalLeaderboardRepository.FriendSnapshot?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    if (snapshot == null) {
        LeaderboardSkeleton()
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (snapshot.insights.isNotEmpty()) {
            item {
                Text(
                    text = "Tin tức từ đối thủ",
                    style = BetterMeTypography.Title.Small.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }
            items(snapshot.insights, key = { it.kind.name + it.rivalDisplayName }) { insight ->
                RivalInsightCard(insight = insight)
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Đối thủ gần bạn",
                    style = BetterMeTypography.Title.Small.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                RefreshPill(isRefreshing = isRefreshing, onClick = onRefresh)
            }
        }
        items(snapshot.entries, key = { it.userId }) { entry ->
            GlobalRow(
                entry = entry,
                highlight = entry.isCurrentUser,
                onTap = { onOpenProfile(entry.userId) }
            )
        }
    }
}

@Composable
private fun WinnersTab(winners: List<MonthlyWinner>) {
    if (winners.isEmpty()) {
        EmptyState(
            emoji = "🏆",
            primary = "Chưa có quán quân được lưu trữ.",
            secondary = "Hoàn thành mùa giải đầu tiên để bắt đầu!"
        )
        return
    }
    val grouped = winners.groupBy { it.seasonKey }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(grouped.keys.toList(), key = { it }) { seasonKey ->
            MonthlyWinnerCard(
                seasonLabel = Season.displayLabel(seasonKey),
                winners = grouped.getValue(seasonKey).sortedBy { it.rank }
            )
        }
    }
}

@Composable
private fun HeadlineRow(
    participantCount: Int,
    seasonLabel: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = seasonLabel,
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${participantCount.toThousands()} người tham gia",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
        RefreshPill(isRefreshing = isRefreshing, onClick = onRefresh)
    }
}

@Composable
private fun RefreshPill(isRefreshing: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(BetterMeColors.Primary.Primary.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
            .clickable(enabled = !isRefreshing) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (isRefreshing) "Đang tải…" else "↻ Tải lại",
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Primary.Primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GlobalRow(
    entry: GlobalLeaderboardEntry,
    highlight: Boolean,
    onTap: () -> Unit
) {
    val accent = BetterMeColors.Primary.Primary
    val podiumTint = when (entry.rank) {
        1 -> Color(0xFFFFF4D6)
        2 -> Color(0xFFEBEEF3)
        3 -> Color(0xFFF6E2CF)
        else -> null
    }
    val bg = when {
        highlight -> accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft)
        podiumTint != null -> podiumTint
        else -> Color.White
    }
    val borderColor = if (highlight) accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium)
    else if (podiumTint != null) Color.Transparent
    else BetterMeColors.Border.BorderLight

    val animatedScore by animateIntAsState(
        targetValue = entry.totalScore,
        animationSpec = tween(durationMillis = 500),
        label = "global_score_${entry.userId}"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(bg)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .clickable { onTap() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RankCircle(rank = entry.rank)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(BetterMeColors.Gray.Gray3)
        ) {
            entry.avatarUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = entry.displayName,
                    modifier = Modifier.size(38.dp).clip(CircleShape)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.displayName + if (entry.isCurrentUser) "  · Bạn" else "",
                    style = BetterMeTypography.Body.Medium,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.size(6.dp))
                LeagueTierBadge(tier = entry.leagueProgress.tier, compact = true)
            }
            if (entry.badges.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                RankBadgeRow(badges = entry.badges)
            }
            Text(
                text = "${entry.totalCompletedHabits} thói quen · ${entry.longestStreak}🔥 · ${entry.completedChallenges}🏆",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            RankDeltaChip(delta = entry.rankDelta)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$animatedScore",
                style = BetterMeTypography.Body.Medium,
                color = if (highlight) accent else BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RankCircle(rank: Int) {
    val (label, bg, fg) = when (rank) {
        1 -> Triple("🥇", Color(0xFFFFF4D6), Color(0xFFB7791F))
        2 -> Triple("🥈", Color(0xFFEBEEF3), Color(0xFF707070))
        3 -> Triple("🥉", Color(0xFFF6E2CF), Color(0xFFA8702E))
        else -> Triple("#$rank", BetterMeColors.Primary.Primary.copy(alpha = 0.08f), BetterMeColors.Primary.Primary)
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "⚠️ $message",
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Red
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                    .background(BetterMeColors.Primary.Primary)
                    .clickable { onRetry() }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Thử lại",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EmptyState(emoji: String, primary: String, secondary: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = emoji, style = BetterMeTypography.Title.Medium.Bold)
            Spacer(Modifier.height(10.dp))
            Text(
                text = primary,
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = secondary,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
    }
}

private fun Int.toThousands(): String {
    if (this < 1000) return this.toString()
    val k = this / 1000.0
    return String.format(java.util.Locale.US, "%.1fK", k)
}

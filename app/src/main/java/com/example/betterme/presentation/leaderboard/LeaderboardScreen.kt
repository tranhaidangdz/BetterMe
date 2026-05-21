package com.example.betterme.presentation.leaderboard

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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.betterme.R
import com.example.betterme.domain.leaderboard.LeaderboardEntry
import com.example.betterme.domain.leaderboard.Season
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.leaderboard.components.LeaderboardSkeleton
import com.example.betterme.presentation.leaderboard.components.MotivationalEventToast
import com.example.betterme.presentation.leaderboard.components.RankBadgeRow
import com.example.betterme.presentation.leaderboard.components.RankDeltaChip
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography
import org.koin.androidx.compose.koinViewModel

/**
 * Full monthly leaderboard screen. Layout:
 *   - top bar with back arrow + challenge title
 *   - month picker pill row (last 6 seasons)
 *   - top-3 podium row (only on the current season AND when there are
 *     at least 3 entries)
 *   - scrollable ranking list with the current user sticky at top
 *     (when outside the top 3 visible range)
 *
 * Behaves like a real competitive list: highlights the user's row
 * visually, shows score deltas implicitly through the gap to the next
 * rank, and renders a tiny "offline" chip when the snapshot is stale.
 */
@Composable
fun LeaderboardScreen(
    challengeId: Int,
    challengeTitle: String,
    onBackClick: () -> Unit,
    viewModel: LeaderboardViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()

    LaunchedEffect(challengeId) {
        viewModel.processIntent(LeaderboardIntent.Initialize(challengeId, challengeTitle))
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
                title = state.challengeTitle.ifBlank { "Bảng xếp hạng" },
                onLeadingClick = onBackClick
            )

            // Motivational toast — surfaces above the season picker so
            // it doesn't push the list. Auto-dismisses after 3.5s; tap
            // collapses it immediately.
            MotivationalEventToast(
                event = state.activeMotivationalEvent,
                onDismiss = {
                    viewModel.processIntent(LeaderboardIntent.DismissMotivationalEvent)
                }
            )

            // Season picker — last 6 months as pills.
            SeasonPickerRow(
                seasons = state.availableSeasons,
                active = state.activeSeasonKey,
                onSelect = { viewModel.processIntent(LeaderboardIntent.SelectSeason(it)) }
            )

            when (val ui = state.ui) {
                LeaderboardUi.Idle, LeaderboardUi.Loading -> LeaderboardSkeleton()
                LeaderboardUi.Empty -> EmptyState()
                is LeaderboardUi.Error -> ErrorState(ui.message, onRetry = {
                    viewModel.processIntent(LeaderboardIntent.Refresh)
                })
                is LeaderboardUi.Success -> SuccessBody(
                    snapshot = ui.snapshot,
                    isRefreshing = state.isRefreshing,
                    isCurrentSeason = Season.isCurrent(state.activeSeasonKey),
                    onRefresh = { viewModel.processIntent(LeaderboardIntent.Refresh) }
                )
            }
        }
    }
}

@Composable
private fun SeasonPickerRow(
    seasons: List<String>,
    active: String,
    onSelect: (String) -> Unit
) {
    if (seasons.isEmpty()) return
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(seasons) { key ->
            val selected = key == active
            val bg = if (selected) BetterMeColors.Primary.Primary
            else BetterMeColors.Primary.Primary.copy(alpha = BetterMeTokens.AccentAlpha.Soft)
            val fg = if (selected) Color.White else BetterMeColors.Primary.Primary
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                    .background(bg)
                    .clickable { onSelect(key) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = Season.displayLabel(key),
                    style = BetterMeTypography.Body.Small.Medium,
                    color = fg,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🌱", fontSize = 36.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Chưa có ai trên bảng xếp hạng tháng này.",
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Check-in thử thách để mở đầu cuộc đua!",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "⚠️ $message",
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
private fun SuccessBody(
    snapshot: com.example.betterme.domain.repository.ChallengeLeaderboardRepository.LeaderboardSnapshot,
    isRefreshing: Boolean,
    isCurrentSeason: Boolean,
    onRefresh: () -> Unit
) {
    val mine = snapshot.myEntry
    val showStickyMine = mine != null && mine.rank > 5 // sticky when outside the visible top 5
    val entries = snapshot.entries

    Column(modifier = Modifier.fillMaxSize()) {

        // Headline + refresh + stale chip.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${snapshot.meta.participantCount.formatThousands()} người tham gia",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextSecondary,
                modifier = Modifier.weight(1f)
            )
            if (snapshot.isStale) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                        .background(BetterMeColors.Gray.Gray3)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "📴 Bản lưu",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = BetterMeColors.Text.TextTertiary
                    )
                }
                Spacer(Modifier.size(8.dp))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                    .background(BetterMeColors.Primary.Primary.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
                    .clickable(enabled = !isRefreshing) { onRefresh() }
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

        // Sticky current-user row when outside the visible top.
        if (showStickyMine) {
            // `showStickyMine` already null-guards `mine`; the requireNotNull
            // here documents that invariant for the reader.
            val sticky = requireNotNull(mine)
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                LeaderboardRow(entry = sticky.copy(isCurrentUser = true), highlight = true)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries, key = { it.userId }) { entry ->
                LeaderboardRow(entry = entry, highlight = entry.isCurrentUser)
            }
            if (!isCurrentSeason) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Đây là mùa đã kết thúc — chỉ xem được, không cập nhật.",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = BetterMeColors.Text.TextTertiary,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntry,
    highlight: Boolean
) {
    val accent = BetterMeColors.Primary.Primary
    // Top 3 get a podium tint so the eye lands there first. The chosen
    // colors mirror the medal palette used inside RankBadge().
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
    val borderColor = when {
        highlight -> accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium)
        podiumTint != null -> Color.Transparent
        else -> BetterMeColors.Border.BorderLight
    }
    // Animate the score so a leaderboard refresh that bumps the user's
    // total feels lively instead of swapping the digits abruptly.
    val animatedScore by animateIntAsState(
        targetValue = entry.totalScore,
        animationSpec = tween(durationMillis = 500),
        label = "score_anim_${entry.userId}"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(bg)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RankBadge(entry.rank)
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    // Explicit parens around each `if/else` are load-bearing:
                    // without them, Kotlin's `if` expression swallows the
                    // trailing `+` and the suspicious-marker is dropped when
                    // isCurrentUser is true.
                    text = entry.displayName +
                        (if (entry.isCurrentUser) "  · Bạn" else "") +
                        (if (entry.isSuspicious) "  ?" else ""),
                    style = BetterMeTypography.Body.Medium,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1
                )
                RankBadgeRow(badges = entry.badges)
            }
            Text(
                text = "${entry.completedTasks} nhiệm vụ · ${entry.currentStreak}🔥",
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
            Text(
                text = "điểm",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
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

private fun Int.formatThousands(): String {
    if (this < 1000) return this.toString()
    val k = this / 1000.0
    return String.format(java.util.Locale.US, "%.1fK", k)
}

package com.example.betterme.presentation.leaderboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.betterme.domain.leaderboard.LeaderboardEntry
import com.example.betterme.domain.repository.ChallengeLeaderboardRepository.LeaderboardSnapshot
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Small embeddable card surfacing the leaderboard's headline:
 *  - 🥇🥈🥉 podium row,
 *  - participant count,
 *  - the current user's rank line (if they have an entry yet).
 *
 * Used on:
 *  - Challenge Overview (one card per joined challenge),
 *  - Challenge Detail (preview above "View Full Leaderboard" CTA).
 *
 * Renders nothing when there are no entries to show — keeps the parent
 * UI clean for challenges with no participants yet.
 */
@Composable
fun LeaderboardSummaryCard(
    snapshot: LeaderboardSnapshot,
    onOpenFullLeaderboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (snapshot.entries.isEmpty()) return
    val top3 = snapshot.entries.take(3)
    val accent = BetterMeColors.Primary.Primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = BetterMeTokens.CardElevation.Body,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero),
                ambientColor = BetterMeTokens.NeutralShadow.Ambient,
                spotColor = BetterMeTokens.NeutralShadow.Spot
            )
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Hero))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White, accent.copy(alpha = 0.05f))
                )
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero)
            )
            .clickable { onOpenFullLeaderboard() }
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🏆", fontSize = 22.sp)
                Spacer(Modifier.size(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bảng xếp hạng tháng",
                        style = BetterMeTypography.Title.Small.Bold,
                        color = BetterMeColors.Text.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${formatCount(snapshot.meta.participantCount)} người tham gia",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = BetterMeColors.Text.TextTertiary
                    )
                }
                Text(
                    text = "Xem tất cả ›",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Podium row — always renders 3 slots even if fewer entries
            // are available so the visual rhythm stays consistent.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                listOf(0, 1, 2).forEach { idx ->
                    val entry = top3.getOrNull(idx)
                    PodiumSlot(
                        medal = MEDALS[idx],
                        entry = entry,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Current user rank line — only when we have an entry and
            // they're outside the top 3 (otherwise their slot is already
            // visible above).
            val mine = snapshot.myEntry
            if (mine != null && mine.rank > 3) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                        .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
                        .border(
                            width = 1.dp,
                            color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                            shape = RoundedCornerShape(BetterMeTokens.CardRadius.Pill)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Bạn đang ở #${mine.rank}",
                            style = BetterMeTypography.Body.Medium,
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${mine.totalScore} điểm",
                            style = BetterMeTypography.Body.Small.Medium,
                            color = accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumSlot(
    medal: String,
    entry: LeaderboardEntry?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = medal, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BetterMeColors.Gray.Gray3)
        ) {
            entry?.avatarUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = entry.displayName,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = entry?.displayName?.take(10) ?: "—",
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = entry?.totalScore?.let { "$it pt" } ?: "",
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary
        )
    }
}

private val MEDALS = listOf("🥇", "🥈", "🥉")

private fun formatCount(n: Int): String {
    if (n < 1000) return n.toString()
    val k = n / 1000.0
    return String.format(java.util.Locale.US, "%.1fK", k)
}

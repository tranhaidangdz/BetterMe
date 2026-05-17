package com.example.betterme.presentation.leaderboard.global.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.betterme.domain.leaderboard.LeaderboardProfile
import com.example.betterme.presentation.leaderboard.components.RankBadgeRow
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Bottom sheet shown when a leaderboard row is tapped. Lightweight
 * read-only profile — no follow, no DM. The user sees who they're
 * competing with + a few headline stats.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardProfileSheet(
    profile: LeaderboardProfile,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BetterMeColors.Gray.Gray3),
                contentAlignment = Alignment.Center
            ) {
                profile.avatarUrl?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = profile.displayName,
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                    )
                } ?: Text(text = "👤", fontSize = 36.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = profile.displayName,
                style = BetterMeTypography.Title.Medium.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Hạng hiện tại: #${profile.currentRank} · ${profile.totalScore} điểm",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
            if (profile.badges.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                RankBadgeRow(badges = profile.badges, maxVisible = 5)
            }

            Spacer(Modifier.height(16.dp))
            LeagueProgressBar(progress = profile.leagueProgress)
            Spacer(Modifier.height(18.dp))

            StatGrid(profile)

            if (profile.favoriteChallengeTitle != null) {
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
                        .background(BetterMeColors.BackGround.BackgroundSecondary)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Thử thách yêu thích: ${profile.favoriteChallengeTitle}",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = BetterMeColors.Text.TextSecondary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatGrid(p: LeaderboardProfile) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCell(label = "Thói quen", value = "${p.totalCompletedHabits}", modifier = Modifier.weight(1f))
        StatCell(label = "Chuỗi dài", value = "${p.longestStreak}🔥", modifier = Modifier.weight(1f))
        StatCell(label = "Hoàn thành", value = "${p.completedChallenges}", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = BetterMeTypography.Title.Small.Bold,
            color = BetterMeColors.Text.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary
        )
    }
}

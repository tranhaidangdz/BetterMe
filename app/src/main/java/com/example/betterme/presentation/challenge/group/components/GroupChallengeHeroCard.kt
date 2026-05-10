package com.example.betterme.presentation.challenge.group.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.categorydetail.components.LinearProgressBar
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun GroupChallengeHeroCard(
    title: String,
    avatars: List<String>,
    overflowCount: Int,
    totalMembers: Int,
    daysCompleted: Int,
    totalDays: Int,
    progressPct: Int,
    hasJoined: Boolean,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A2540))
            .padding(20.dp)
    ) {
        Text(
            text = title,
            style = BetterMeTypography.Title.Medium.Bold,
            color = Color.White,
            maxLines = 2
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarStack(emojis = avatars, overflowCount = overflowCount)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${formatCount(totalMembers)} thành viên",
                style = BetterMeTypography.Body.Small.Medium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tiến độ nhóm",
                style = BetterMeTypography.Body.Small.Medium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = "$daysCompleted/$totalDays ngày",
                style = BetterMeTypography.Body.Small.Medium,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressBar(
            percent = progressPct / 100f,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (hasJoined) Color.White.copy(alpha = 0.2f) else Color.White)
                .clickable(enabled = !hasJoined) { onJoin() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (hasJoined) "Đã tham gia" else "Tham gia thử thách",
                style = BetterMeTypography.Title.Small.Bold,
                color = if (hasJoined) Color.White else Color(0xFF1A2540)
            )
        }
    }
}

private fun formatCount(n: Int): String {
    if (n >= 1000) return "${"%.1f".format(n / 1000f)}K"
    return n.toString()
}

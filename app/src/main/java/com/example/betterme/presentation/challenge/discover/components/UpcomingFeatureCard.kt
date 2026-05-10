package com.example.betterme.presentation.challenge.discover.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Hero card used by the Discovery screen's "Sắp diễn ra" carousel. Larger than
 * UpcomingChallengeRow — designed for visual impact with a gradient banner, the start
 * date written out, a live countdown ("Còn 12 ngày") and the participant count.
 */
@Composable
fun UpcomingFeatureCard(
    title: String,
    iconEmoji: String,
    accentColor: Color,
    daysUntilStart: Int,
    startLabel: String,             // "01/02/2026"
    rewardCoins: Int,
    participantCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(260.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(accentColor, accentColor.copy(alpha = 0.72f))
                )
            )
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = iconEmoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Sắp diễn ra",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = BetterMeTypography.Title.Medium.Bold,
            color = Color.White,
            maxLines = 2,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Bắt đầu $startLabel",
            style = BetterMeTypography.Body.Small.Medium,
            color = Color.White.copy(alpha = 0.88f)
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (daysUntilStart <= 0) "Hôm nay!"
                    else "Còn $daysUntilStart ngày",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "🪙 +$rewardCoins  ·  👥 ${formatCount(participantCount)}",
                style = BetterMeTypography.Body.Small.Medium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

private fun formatCount(n: Int): String =
    if (n >= 1000) "${"%.1f".format(n / 1000f)}K" else n.toString()

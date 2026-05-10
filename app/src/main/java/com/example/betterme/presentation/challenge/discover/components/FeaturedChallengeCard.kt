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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.challenge.model.FeaturedChallengeUiModel
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun FeaturedChallengeCard(
    model: FeaturedChallengeUiModel,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(240.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        model.accentColor,
                        model.accentColor.copy(alpha = 0.7f)
                    )
                )
            )
            .clickable { onClick(model.challengeId) }
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = model.iconEmoji, fontSize = 64.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = model.title,
            style = BetterMeTypography.Title.Small.Bold,
            color = Color.White,
            maxLines = 2
        )
        Text(
            text = model.durationDaysLabel,
            style = BetterMeTypography.Body.Small.Medium,
            color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "🪙 ${model.rewardCoins} xu", color = Color.White, style = BetterMeTypography.Body.Small.Medium)
            Text(text = "·", color = Color.White.copy(alpha = 0.6f))
            Text(
                text = "${formatCount(model.participantCount)} tham gia",
                color = Color.White.copy(alpha = 0.85f),
                style = BetterMeTypography.Body.Small.Medium
            )
        }
    }
}

private fun formatCount(n: Int): String {
    if (n >= 1000) return "${"%.1f".format(n / 1000f)}K"
    return n.toString()
}

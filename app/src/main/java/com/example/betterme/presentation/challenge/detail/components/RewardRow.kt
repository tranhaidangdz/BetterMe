package com.example.betterme.presentation.challenge.detail.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Reward strip on the Challenge Detail screen — two horizontal cells (coin / badge)
 * separated by a thin divider. Each cell is icon-on-the-left + text on the right (label
 * above value), matching the supplied design.
 */
@Composable
fun RewardRow(
    coins: Int,
    badgeName: String?,
    modifier: Modifier = Modifier,
    badgeImageUrl: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0D000000),
                spotColor = Color(0x1A000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(BetterMeColors.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RewardCell(
            iconBg = Color(0xFFFEF3C7),
            iconEmoji = "🪙",
            value = "$coins",
            label = "Xu",
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .height(48.dp)
                .width(1.dp)
                .background(BetterMeColors.Border.BorderLight)
        )
        RewardCell(
            iconBg = Color(0xFFDBEAFE),
            iconEmoji = "💧",
            iconUrl = badgeImageUrl,
            value = badgeName ?: "—",
            label = "Huy hiệu",
            valueOnTop = false,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
    }
}

@Composable
private fun RewardCell(
    iconBg: Color,
    iconEmoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueOnTop: Boolean = true,
    iconUrl: String? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            if (!iconUrl.isNullOrBlank()) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Text(text = iconEmoji, fontSize = 20.sp)
            }
        }
        Column {
            if (valueOnTop) {
                Text(
                    text = value,
                    style = BetterMeTypography.Title.Medium.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
            } else {
                Text(
                    text = label,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
                Text(
                    text = value,
                    style = BetterMeTypography.Title.Small.Bold,
                    color = BetterMeColors.Text.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

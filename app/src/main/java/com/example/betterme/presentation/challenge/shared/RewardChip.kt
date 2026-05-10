package com.example.betterme.presentation.challenge.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

enum class ChipTheme { Light, Dark }

/**
 * Compact reward chip — coin row, badge row, or a combined "+50 xu  ·  🏅 Huy hiệu"
 * line. Pass theme=Dark for the navy celebration modal.
 */
@Composable
fun RewardChip(
    coins: Int? = null,
    badgeName: String? = null,
    theme: ChipTheme = ChipTheme.Light,
    modifier: Modifier = Modifier
) {
    val bg = when (theme) {
        ChipTheme.Light -> BetterMeColors.BackGround.BackgroundSecondary
        ChipTheme.Dark -> Color(0x1FFFFFFF)
    }
    val textColor = when (theme) {
        ChipTheme.Light -> BetterMeColors.Text.TextPrimary
        ChipTheme.Dark -> BetterMeColors.White
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (coins != null) {
            Text(text = "🪙", fontSize = 14.sp)
            Text(
                text = "+$coins xu",
                style = BetterMeTypography.Body.Small.Medium,
                color = textColor
            )
        }
        if (coins != null && badgeName != null) {
            Text(text = "·", color = textColor.copy(alpha = 0.5f))
        }
        if (badgeName != null) {
            Text(text = "🏅", fontSize = 14.sp)
            Text(
                text = "Huy hiệu $badgeName",
                style = BetterMeTypography.Body.Small.Medium,
                color = textColor
            )
        }
    }
}

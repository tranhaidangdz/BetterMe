package com.example.betterme.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.home.model.CantMiss
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * "Đang thực hiện" carousel card. White surface, accent-tinted border, neutral
 * shadow — same grammar as HomeCard and the Habit Group rows so the whole screen
 * reads as one design system instead of three different card families.
 *
 * Accent identity is carried via: the border, the category-icon disc, the progress
 * bar, and the percent pill. The card body itself is fully readable on every
 * category — the previous gradient drift made low-percent rows look "washed out".
 */
@Composable
fun CantMissCard(
    item: CantMiss,
    cardColor: Color,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(180.dp)
            .shadow(
                elevation = BetterMeTokens.CardElevation.Body,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Standard),
                ambientColor = BetterMeTokens.NeutralShadow.Ambient,
                spotColor = BetterMeTokens.NeutralShadow.Spot
            )
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Standard))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = cardColor.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Standard)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(cardColor.copy(alpha = BetterMeTokens.AccentAlpha.Soft)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.categoryIcon,
                    style = BetterMeTypography.Body.Small.Medium
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.categoryName,
                style = BetterMeTypography.Body.Small.Medium,
                color = cardColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = item.habitTitle,
            style = BetterMeTypography.Title.Small.SemiBold,
            color = BetterMeColors.Text.TextPrimary,
            maxLines = 2,
            modifier = Modifier.heightIn(min = 40.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { item.progress / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill)),
            color = cardColor,
            trackColor = cardColor.copy(alpha = BetterMeTokens.AccentAlpha.Soft)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tiến độ",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                    .background(cardColor.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${item.progress}%",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = cardColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

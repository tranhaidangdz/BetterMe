package com.example.betterme.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.home.HomeCategoryGroup
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Home category tile.
 *
 * Solid white surface, 1dp accent-tinted border, neutral shadow. Replaces the previous
 * white→accent gradient + accent-tinted shadow combo that read as washed out against
 * the pale-blue page background. Category identity is carried via the border, the
 * tinted emoji disc, and the accent-tinted chevron pill — three discreet signals
 * instead of a body-wide gradient.
 */
@Composable
fun HomeCard(
    index: Int,
    group: HomeCategoryGroup,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
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
                color = group.color.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Standard)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tinted emoji disc — identity glyph for the category.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(group.color.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
                .border(
                    width = 1.dp,
                    color = group.color.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = group.categoryIcon, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.categoryName,
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${group.habitCount} nhiệm vụ",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                .background(group.color.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "›",
                style = BetterMeTypography.Title.Small.Bold,
                color = group.color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

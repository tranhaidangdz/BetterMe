package com.example.betterme.presentation.home.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.home.HomeCategoryGroup
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Premium category tile for the Home screen.
 *
 * Visual identity per category is carried by the accent color piped in from
 * [HomeCategoryGroup.color]: a soft vertical surface gradient inside a white card
 * (so each category reads as its own visual identity but the layout stays unified),
 * the category emoji in a tinted disc on the left, and a discreet "N nhiệm vụ" chip
 * on the right. Soft elevation lifts the card off the page without flat-color glare.
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
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = group.color.copy(alpha = 0.18f),
                spotColor = group.color.copy(alpha = 0.22f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        group.color.copy(alpha = 0.08f)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Emoji disc. Soft tinted disc that reads as the category's identity glyph;
        // replaces the prior numbered-circle which felt clinical against the new layout.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            group.color.copy(alpha = 0.28f),
                            group.color.copy(alpha = 0.14f)
                        )
                    )
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

        // Count chip. Reads as the active hit-target: tap the card and you land on the
        // category's habit list. Color-coordinated with the accent so the visual
        // grouping feels intentional instead of arbitrary.
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(group.color.copy(alpha = 0.16f))
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

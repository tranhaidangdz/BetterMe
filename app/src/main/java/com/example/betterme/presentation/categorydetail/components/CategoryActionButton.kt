package com.example.betterme.presentation.categorydetail.components

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
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Action row at the bottom of the Habit Group screen ("AI nhận xét", "Thêm thói quen",
 * "AI gợi ý"). Visually paired with the rest of the redesigned screen — white surface,
 * soft category-tinted gradient, accent-tinted shadow, tinted emoji disc, and a
 * subtle chevron pill on the right.
 *
 * The [accentColor] is piped from the category palette so each group's action rows
 * inherit the same identity color as its summary and habit cards. Falls back to a
 * neutral blue tint when no accent is supplied.
 */
@Composable
fun CategoryActionButton(
    icon: String,
    label: String,
    onClick: () -> Unit,
    accentColor: Color = BetterMeColors.Primary.Primary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = accentColor.copy(alpha = 0.16f),
                spotColor = accentColor.copy(alpha = 0.20f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        accentColor.copy(alpha = 0.06f)
                    )
                )
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 18.sp)
        }
        Text(
            text = label,
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(accentColor.copy(alpha = 0.14f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "›",
                style = BetterMeTypography.Title.Small.Bold,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

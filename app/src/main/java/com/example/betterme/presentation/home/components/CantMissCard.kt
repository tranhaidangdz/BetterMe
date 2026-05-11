package com.example.betterme.presentation.home.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.home.model.CantMiss
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Carousel card for the Home "Đang thực hiện" rail.
 *
 * White surface with a soft category-tinted vertical gradient, a small accent disc
 * carrying the category icon, the habit title, a thin animated progress bar, and a
 * percentage pill anchored on the right. Soft elevation gives the cards visible
 * depth as the user scrolls horizontally.
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
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = cardColor.copy(alpha = 0.18f),
                spotColor = cardColor.copy(alpha = 0.22f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        cardColor.copy(alpha = 0.08f)
                    )
                )
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(cardColor.copy(alpha = 0.18f)),
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
                .clip(RoundedCornerShape(999.dp)),
            color = cardColor,
            trackColor = cardColor.copy(alpha = 0.18f)
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
                    .clip(RoundedCornerShape(999.dp))
                    .background(cardColor.copy(alpha = 0.14f))
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

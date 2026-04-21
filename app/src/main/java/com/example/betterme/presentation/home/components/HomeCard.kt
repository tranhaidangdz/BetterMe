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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.home.HomeCategoryGroup
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun HomeCard(
    index: Int,
    group: HomeCategoryGroup,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cardBackground = group.color.copy(alpha = 0.12f)
    val cardBorder = group.color.copy(alpha = 0.30f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackground)
            .border(
                width = 1.dp,
                color = cardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(group.color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${index + 1}",
                style = BetterMeTypography.Title.Small.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.categoryName,
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                maxLines = 1
            )
            Text(
                text = "${group.habitCount} nhiệm vụ",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
        Text(
            text = group.categoryIcon,
            style = BetterMeTypography.Headline.Small.Bold
        )
    }
}

package com.example.betterme.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.home.model.CantMiss
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun CantMissCard(
    item: CantMiss,
    cardColor: Color,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(170.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardColor.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(
            text = item.categoryName,
            style = BetterMeTypography.Body.Small.Medium,
            color = cardColor,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.habitTitle,
            style = BetterMeTypography.Title.Small.SemiBold,
            color = BetterMeColors.Text.TextPrimary,
            maxLines = 2,
            modifier = Modifier.heightIn(min = 40.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { item.progress / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = cardColor,
            trackColor = cardColor.copy(alpha = 0.2f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${item.progress}%",
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary
        )
    }
}

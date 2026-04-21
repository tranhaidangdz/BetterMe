package com.example.betterme.presentation.categorydetail.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun CircularProgressLabel(
    percent: Int,
    size: Dp = 72.dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.size(size),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeWidth = 7.dp,
            strokeCap = StrokeCap.Round
        )
        Text(
            text = "$percent%",
            style = BetterMeTypography.Body.Small.Medium,
            color = color
        )
    }
}

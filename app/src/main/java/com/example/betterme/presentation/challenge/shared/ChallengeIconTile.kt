package com.example.betterme.presentation.challenge.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Rounded square emoji tile used as the leading icon on cards and rows.
 * The bg color is the challenge's color_hex with reduced alpha for a soft pastel feel.
 */
@Composable
fun ChallengeIconTile(
    emoji: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    cornerRadius: Dp = 14.dp,
    emojiSize: androidx.compose.ui.unit.TextUnit = 24.sp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(accentColor.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = emojiSize)
    }
}

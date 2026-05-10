package com.example.betterme.presentation.challenge.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun DifficultyPill(
    difficulty: Difficulty,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(difficulty.bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = difficulty.label,
            style = BetterMeTypography.Body.Small.Medium,
            color = difficulty.color
        )
    }
}

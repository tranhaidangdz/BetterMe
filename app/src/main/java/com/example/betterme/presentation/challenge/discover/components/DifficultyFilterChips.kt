package com.example.betterme.presentation.challenge.discover.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.challenge.shared.Difficulty
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Horizontal filter row of difficulty pills shown above the category chips on the
 * Challenge Discovery screen. Tapping the same chip again deselects it (passes null).
 */
@Composable
fun DifficultyFilterChips(
    selected: Difficulty?,
    onSelect: (Difficulty?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            Chip(
                label = "Mọi mức độ",
                selectedColor = BetterMeColors.Primary.Primary,
                isSelected = selected == null,
                onClick = { onSelect(null) }
            )
        }
        items(Difficulty.entries.toList()) { difficulty ->
            Chip(
                label = difficulty.label,
                selectedColor = difficulty.color,
                isSelected = selected == difficulty,
                onClick = {
                    onSelect(if (selected == difficulty) null else difficulty)
                }
            )
        }
    }
}

@Composable
private fun Chip(
    label: String,
    selectedColor: androidx.compose.ui.graphics.Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (isSelected) selectedColor
                else selectedColor.copy(alpha = 0.12f)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = if (isSelected) BetterMeColors.White else selectedColor
        )
    }
}

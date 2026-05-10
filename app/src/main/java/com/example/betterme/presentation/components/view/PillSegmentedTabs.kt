package com.example.betterme.presentation.components.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Generic horizontal pill-segment control. Replaces the hard-coded `FilterTabs`
 * for any feature that needs an enum-driven tab row.
 *
 * Selected pill: solid primary blue with white text.
 * Unselected: 16% alpha primary with primary text.
 */
@Composable
fun <T> PillSegmentedTabs(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val isSelected = item == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isSelected) BetterMeColors.Primary.Primary
                        else BetterMeColors.Primary.Primary.copy(alpha = 0.16f)
                    )
                    .clickable { onSelect(item) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = label(item),
                    style = BetterMeTypography.Body.Small.Medium,
                    color = if (isSelected) BetterMeColors.White else BetterMeColors.Primary.Primary
                )
            }
        }
    }
}

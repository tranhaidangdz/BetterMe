package com.example.betterme.presentation.dailyhabits.components

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
import com.example.betterme.presentation.dailyhabits.DailyHabitFilter
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun FilterTabs(
    selectedFilter: DailyHabitFilter,
    onSelect: (DailyHabitFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DailyHabitFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isSelected) BetterMeColors.Primary.Primary
                        else BetterMeColors.Primary.Primary.copy(alpha = 0.16f)
                    )
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = filter.label,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = if (isSelected) BetterMeColors.White else BetterMeColors.Primary.Primary
                )
            }
        }
    }
}

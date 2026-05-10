package com.example.betterme.presentation.challenge.badges.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.challenge.badges.BadgeStatusFilter
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Pill-style filter row above the badge sections — All / Earned / Locked. Tapping a pill
 * narrows the visible badges in each section. Empty sections are hidden by the screen so
 * the filter never leaves a "ghost" heading with no content.
 */
@Composable
fun BadgeStatusFilterRow(
    selected: BadgeStatusFilter,
    earnedCount: Int,
    lockedCount: Int,
    onSelect: (BadgeStatusFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val total = earnedCount + lockedCount
    val items = listOf(
        BadgeStatusFilter.All to "Tất cả ($total)",
        BadgeStatusFilter.Earned to "Đã đạt ($earnedCount)",
        BadgeStatusFilter.Locked to "Chưa đạt ($lockedCount)"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (filter, label) ->
            val isSelected = filter == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isSelected) BetterMeColors.Primary.Primary
                        else BetterMeColors.White
                    )
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = if (isSelected) BetterMeColors.White
                    else BetterMeColors.Text.TextPrimary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

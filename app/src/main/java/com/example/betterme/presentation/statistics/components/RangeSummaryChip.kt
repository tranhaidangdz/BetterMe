package com.example.betterme.presentation.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import com.example.betterme.presentation.statistics.StatisticsTab
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Row shown directly under the tab selector. Renders the effective date range for the
 * current tab — clickable to launch the [DateRangePickerSheet]. When the user has an
 * active CUSTOM range, a "Reset" trailing action restores the tab's natural range.
 *
 * For non-CUSTOM tabs the chip is informational only ("21/01 → 27/01"); clicking it
 * opens the picker, which is a faster path to a one-off range than scrolling tabs.
 */
@Composable
fun RangeSummaryChip(
    selectedTab: StatisticsTab,
    effectiveStart: Long,
    effectiveEnd: Long,
    onClick: () -> Unit,
    onReset: () -> Unit
) {
    val isCustom = selectedTab == StatisticsTab.CUSTOM
    val accent = BetterMeColors.Primary.Primary
    val label = "${formatShort(effectiveStart)} → ${formatShort(effectiveEnd)}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Pill)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "📅",
            style = BetterMeTypography.Body.Medium
        )
        Text(
            text = label,
            style = BetterMeTypography.Body.Medium,
            color = accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (isCustom) {
            Text(
                text = "Đặt lại",
                style = BetterMeTypography.Body.Small.Medium,
                color = accent,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                    .clickable { onReset() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        } else {
            Text(
                text = "Đổi",
                style = BetterMeTypography.Body.Small.Medium,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatShort(ms: Long): String {
    if (ms <= 0) return "—"
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.forLanguageTag("vi"))
    return sdf.format(java.util.Date(ms))
}

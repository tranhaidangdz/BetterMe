package com.example.betterme.presentation.components.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Reusable "Title — trailing link →" row used by Discover, Achievements, etc.
 *
 * If [trailingLabel] is null, the row only renders the title.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingLabel: String? = null,
    onTrailingClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = BetterMeTypography.Title.Medium.Bold,
            color = BetterMeColors.Text.TextPrimary
        )
        if (trailingLabel != null) {
            Text(
                text = trailingLabel,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Primary.Primary,
                modifier = Modifier.clickable { onTrailingClick() }
            )
        }
    }
}

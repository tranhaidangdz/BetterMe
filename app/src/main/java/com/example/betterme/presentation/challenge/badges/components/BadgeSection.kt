package com.example.betterme.presentation.challenge.badges.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.challenge.badges.BadgeSectionUi
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * One titled section of the Badge Collection screen.
 *
 * Renders a numbered heading ("1. Huy hiệu cơ bản") above a single white card containing
 * up to 5 badges per row, distributed evenly with [Arrangement.SpaceEvenly] so all tiles
 * share the same column width regardless of how many badges the section has. Soft shadow
 * matches the rest of the app's card styling.
 */
@Composable
fun BadgeSection(
    section: BadgeSectionUi,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = section.title,
            style = BetterMeTypography.Title.Small.Bold,
            color = BetterMeColors.Primary.Primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color(0x0D000000),
                    spotColor = Color(0x1A000000)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(BetterMeColors.White)
                .padding(vertical = 16.dp)
        ) {
            section.badges.chunked(5).forEachIndexed { rowIndex, rowBadges ->
                if (rowIndex > 0) Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowBadges.forEach { badge ->
                        BadgeGridItem(model = badge)
                    }
                    // Pad short rows so the column widths stay constant.
                    repeat(5 - rowBadges.size) {
                        Spacer(modifier = Modifier.width(64.dp))
                    }
                }
            }
        }
    }
}

package com.example.betterme.presentation.challenge.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Three-column stat card on the Challenge Detail screen — matches the design's
 * "Tiến độ / Tỷ lệ hoàn thành / Thời gian còn lại" layout. Each cell is a small label on
 * top, the big value (accent color), and an optional unit underneath. Soft shadow + white
 * card to lift it off the background.
 */
@Composable
fun ChallengeStatsRow(
    completedDays: Int,
    totalDays: Int,
    completionPct: Int,
    daysRemaining: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0D000000),
                spotColor = Color(0x1A000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(BetterMeColors.White)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatCell(
            header = "Tiến độ",
            value = "$completedDays/$totalDays",
            unit = "ngày",
            accent = accentColor,
            modifier = Modifier.weight(1f)
        )
        VDivider()
        StatCell(
            header = "Tỷ lệ hoàn thành",
            value = "$completionPct%",
            unit = "",
            accent = accentColor,
            modifier = Modifier.weight(1f)
        )
        VDivider()
        StatCell(
            header = "Thời gian còn lại",
            value = "$daysRemaining",
            unit = "ngày",
            accent = accentColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCell(
    header: String,
    value: String,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = header,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = BetterMeTypography.Headline.Small.Bold,
            color = accent,
            fontWeight = FontWeight.Bold
        )
        if (unit.isNotBlank()) {
            Text(
                text = unit,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
    }
}

@Composable
private fun VDivider() {
    Box(
        modifier = Modifier
            .height(54.dp)
            .width(1.dp)
            .background(BetterMeColors.Border.BorderLight)
    )
}

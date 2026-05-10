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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

data class MilestoneUi(
    val percent: Int,           // 25 / 50 / 75 / 100
    val label: String,          // "Khởi động", "Vượt nửa chặng", ...
    val isReached: Boolean,
    val emoji: String           // "🌱", "🔥", "🚀", "🏆"
)

/**
 * 4-step milestone bar (25/50/75/100%) shown on Challenge Detail. Reached steps render
 * full-color with the accent; locked steps stay gray. The connecting line between dots
 * fills proportionally to the user's progress for a clear sense of travel.
 */
@Composable
fun MilestoneRow(
    milestones: List<MilestoneUi>,
    progressPct: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
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
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        // Connecting track
        Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(4.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BetterMeColors.Gray.Gray3)
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(progressPct.coerceIn(0, 100) / 100f)
                    .height(4.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                milestones.forEach { m ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (m.isReached) accentColor
                                else BetterMeColors.Gray.Gray3
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = m.emoji,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            milestones.forEach { m ->
                Column(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${m.percent}%",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = if (m.isReached) accentColor else BetterMeColors.Text.TextTertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = m.label,
                        style = BetterMeTypography.Body.Small.Medium,
                        color = BetterMeColors.Text.TextTertiary
                    )
                }
            }
        }
    }
}

/**
 * Default milestone set (25 / 50 / 75 / 100%) with localized Vietnamese labels.
 */
fun defaultMilestones(progressPct: Int): List<MilestoneUi> = listOf(
    MilestoneUi(25, "Khởi động", progressPct >= 25, "🌱"),
    MilestoneUi(50, "Nửa chặng", progressPct >= 50, "🔥"),
    MilestoneUi(75, "Bứt phá", progressPct >= 75, "🚀"),
    MilestoneUi(100, "Hoàn thành", progressPct >= 100, "🏆")
)

package com.example.betterme.presentation.habitdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.habitdetail.CheckInLogUiModel
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun CheckInLogCard(
    log: CheckInLogUiModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BetterMeColors.White)
            .padding(14.dp)
    ) {
        // Header: date + status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = log.dateFormatted,
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (log.status == "DONE") BetterMeColors.Green.copy(alpha = 0.15f)
                        else BetterMeColors.Red.copy(alpha = 0.12f)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (log.status == "DONE") "Đã hoàn thành" else "Bỏ lỡ",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = if (log.status == "DONE") BetterMeColors.Green else BetterMeColors.Red
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Time + check-in info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = log.timeFormatted,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
            Text(
                text = if (log.status == "DONE") "Đã check in" else "Chưa check in",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextPrimary
            )
        }

        // Note
        if (!log.note.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = log.note,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary,
                maxLines = 3
            )
        }
    }
}

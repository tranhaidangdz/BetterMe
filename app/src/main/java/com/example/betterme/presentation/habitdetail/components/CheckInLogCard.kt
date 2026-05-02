package com.example.betterme.presentation.habitdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.habitdetail.CheckInLogUiModel
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun CheckInLogCard(
    log: CheckInLogUiModel,
    modifier: Modifier = Modifier
) {
    val isDone = log.status == "DONE"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BetterMeColors.White)
            .padding(14.dp)
    ) {
        // Header row: date + status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date — takes available space, truncates if needed
            Text(
                text = log.dateFormatted,
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Status badge — compact
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isDone) BetterMeColors.Green.copy(alpha = 0.12f)
                        else BetterMeColors.Red.copy(alpha = 0.10f)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isDone) BetterMeColors.Green else BetterMeColors.Red)
                )
                Text(
                    text = if (isDone) "Hoàn thành" else "Bỏ lỡ",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = if (isDone) BetterMeColors.Green else BetterMeColors.Red,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Time + action
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
                text = if (isDone) "Đã check in" else "Chưa check in",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextPrimary
            )
        }

        // Note (if any)
        if (!log.note.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = log.note,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

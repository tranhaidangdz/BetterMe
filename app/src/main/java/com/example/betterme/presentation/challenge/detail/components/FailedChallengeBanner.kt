package com.example.betterme.presentation.challenge.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Persistent red banner shown above the history strip when a challenge is FAILED.
 * Communicates two things:
 *  - The user can still check in for history.
 *  - The status will not change back to in-progress / completed.
 *
 * Date labels are formatted with `Locale.forLanguageTag("vi")` so the dd/MM/yyyy order
 * matches the rest of the app.
 */
@Composable
fun FailedChallengeBanner(
    failedAtDate: Long?,
    targetEndDate: Long?,
    modifier: Modifier = Modifier
) {
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi")) }
    val missedLabel = failedAtDate?.let { fmt.format(Date(it)) }
    val endLabel = targetEndDate?.let { fmt.format(Date(it)) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BetterMeColors.Red.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "🛑", fontSize = 24.sp)
        Column {
            Text(
                text = "Thử thách đã thất bại",
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Red,
                fontWeight = FontWeight.Bold
            )
            val subtitle = buildString {
                append("Bạn đã bỏ lỡ ngày check-in")
                if (missedLabel != null) append(" ($missedLabel)")
                if (endLabel != null) append(". Kết thúc dự kiến $endLabel")
                append(". Bạn vẫn có thể check-in để lưu lịch sử, nhưng trạng thái sẽ không đổi.")
            }
            Text(
                text = subtitle,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextSecondary
            )
        }
    }
}

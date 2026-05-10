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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * High-impact streak summary shown at the top of the active Challenge Detail screen.
 *
 * Renders on a gradient surface tinted with the challenge's accent color and surfaces
 * the three numbers users care about most:
 * - **Current streak** (large + flame emoji) — drives urgency to keep checking in.
 * - **Best streak** — personal record beneath the current number.
 * - **Days left until the journey is complete**.
 *
 * A short tier-aware motivational line at the bottom keeps the surface from looking like
 * a static stats block.
 */
@Composable
fun StreakCounterCard(
    currentStreak: Int,
    bestStreak: Int,
    daysRemaining: Int,
    motivationalText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accentColor,
                        accentColor.copy(alpha = 0.78f)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🔥", fontSize = 30.sp)
            }
            Spacer(modifier = Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$currentStreak ngày liên tiếp",
                    style = BetterMeTypography.Headline.Small.Bold,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Kỷ lục: $bestStreak ngày",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = Color.White.copy(alpha = 0.82f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$daysRemaining",
                    style = BetterMeTypography.Headline.Small.Bold,
                    color = Color.White
                )
                Text(
                    text = "ngày còn lại",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = Color.White.copy(alpha = 0.82f)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = motivationalText,
                style = BetterMeTypography.Body.Small.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Returns a tier-aware motivational line based on `progressPct` and difficulty raw value
 * (EASY / MEDIUM / HARD / LEGENDARY). Caller passes the raw string so this stays decoupled
 * from the [com.example.betterme.presentation.challenge.shared.Difficulty] enum.
 */
fun motivationalForProgress(progressPct: Int, difficultyRaw: String): String {
    val isLegendary = difficultyRaw == "LEGENDARY"
    return when {
        progressPct == 0 -> if (isLegendary)
            "Một hành trình huyền thoại bắt đầu từ ngày đầu tiên." else
            "Bắt đầu ngay hôm nay — bước đầu là bước khó nhất."
        progressPct < 25 -> "Đẹp! Bạn đã khởi động. Giữ nhịp mỗi ngày nhé."
        progressPct < 50 -> "Đà đang lên. Hãy cứ duy trì kỷ luật bạn đang có."
        progressPct < 75 -> "Đã quá nửa chặng đường — gần đỉnh rồi!"
        progressPct < 100 -> if (isLegendary)
            "Chỉ còn vài ngày để chạm tới huy hiệu huyền thoại." else
            "Sắp hoàn thành. Đừng để công sức bao nhiêu ngày qua bị uổng."
        else -> "Bạn đã chinh phục thử thách này. Tự hào về bạn 🏆"
    }
}

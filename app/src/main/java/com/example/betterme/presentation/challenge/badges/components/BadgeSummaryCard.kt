package com.example.betterme.presentation.challenge.badges.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Header card on the Badge Collection screen showing earned vs. total. Gradient banner
 * with a large fraction read-out, a filled progress bar, and a friendly subtitle that
 * adapts to the user's progress. Lives at the top of the LazyColumn above the section list.
 */
@Composable
fun BadgeSummaryCard(
    earned: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val percent = if (total == 0) 0 else ((earned.toFloat() / total) * 100f).toInt()
    val animated by animateFloatAsState(
        targetValue = if (total == 0) 0f else (earned.toFloat() / total).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900),
        label = "badge-summary"
    )

    val subtitle = when {
        total == 0 -> "Chưa có huy hiệu nào trong hệ thống"
        earned == 0 -> "Hoàn thành thử thách đầu tiên để mở khóa huy hiệu!"
        earned >= total -> "Hoàn hảo! Bạn đã sưu tầm hết tất cả huy hiệu 🎉"
        percent >= 75 -> "Còn ${total - earned} huy hiệu nữa thôi, cố lên!"
        percent >= 50 -> "Đã đi được nửa chặng đường rồi, tuyệt vời!"
        else -> "Tiếp tục hành trình để thu thập thêm huy hiệu"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0x14000000),
                spotColor = Color(0x29000000)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
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
                Text(text = "🏆", fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bộ sưu tập huy hiệu",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$earned",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = " / $total",
                        style = BetterMeTypography.Title.Medium.Bold,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$percent%",
                    style = BetterMeTypography.Title.Small.Bold,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.22f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = subtitle,
            style = BetterMeTypography.Body.Small.Medium,
            color = Color.White.copy(alpha = 0.92f)
        )
    }
}

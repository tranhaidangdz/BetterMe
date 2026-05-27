package com.example.betterme.presentation.challenge.achievements.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.challenge.model.BadgeUiModel
import com.example.betterme.presentation.challenge.shared.isValidDrawableRes

/**
 * Compact row of up to 4 earned badges on the Achievements profile screen.
 * Pads with locked-style placeholders so the row always has 4 cells.
 */
@Composable
fun BadgeCollectionRow(
    badges: List<BadgeUiModel>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        badges.forEach { badge ->
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                // Guard against stale `R.drawable` IDs persisted in Room: a
                // non-zero id that no longer resolves in this build would crash
                // painterResource. Fall back to the emoji when invalid.
                if (isValidDrawableRes(badge.iconRes)) {
                    Image(
                        painter = painterResource(badge.iconRes),
                        contentDescription = badge.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(56.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(badge.accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = badge.iconEmoji, fontSize = 26.sp)
                    }
                }
            }
        }
        repeat((4 - badges.size).coerceAtLeast(0)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEEEEEF)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🔒", fontSize = 22.sp)
            }
        }
    }
}

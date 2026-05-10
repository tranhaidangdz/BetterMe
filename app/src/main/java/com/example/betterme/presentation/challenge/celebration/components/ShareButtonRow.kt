package com.example.betterme.presentation.challenge.celebration.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.theme.BetterMeTypography

enum class SharePlatform(val label: String, val emoji: String, val color: Color) {
    Facebook("Facebook", "f", Color(0xFF1877F2)),
    Zalo("Zalo", "Z", Color(0xFF0068FF)),
    Instagram("Instagram", "📷", Color(0xFFE4405F)),
    Other("Khác", "···", Color(0xFF64748B))
}

@Composable
fun ShareButtonRow(
    onShare: (SharePlatform) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterHorizontally)
    ) {
        SharePlatform.entries.forEach { platform ->
            ShareButton(platform, onClick = { onShare(platform) })
        }
    }
}

@Composable
private fun ShareButton(platform: SharePlatform, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(platform.color)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = platform.emoji, color = Color.White, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = platform.label,
            style = BetterMeTypography.Body.Small.Medium,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

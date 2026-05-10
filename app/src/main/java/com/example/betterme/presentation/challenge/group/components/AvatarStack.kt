package com.example.betterme.presentation.challenge.group.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
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
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun AvatarStack(
    emojis: List<String>,
    overflowCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        emojis.forEachIndexed { i, emoji ->
            Box(
                modifier = Modifier
                    .offset(x = (i * -8).dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE4E4E6)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 16.sp)
            }
        }
        if (overflowCount > 0) {
            Box(
                modifier = Modifier
                    .offset(x = (emojis.size * -8).dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BetterMeColors.Primary.Primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$overflowCount",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = Color.White
                )
            }
        }
    }
}

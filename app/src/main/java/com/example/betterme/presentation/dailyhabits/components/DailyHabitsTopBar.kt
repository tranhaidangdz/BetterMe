package com.example.betterme.presentation.dailyhabits.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun DailyHabitsTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint = BetterMeColors.Text.TextPrimary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = "Thói quen",
            style = BetterMeTypography.Title.Medium.Bold,
            color = BetterMeColors.Text.TextPrimary
        )
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Notification",
            tint = BetterMeColors.Text.TextPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}

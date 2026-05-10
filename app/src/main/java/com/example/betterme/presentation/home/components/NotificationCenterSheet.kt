package com.example.betterme.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.data.local.room.entities.NotificationEntity
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bottom-sheet style notification center. Each row is tappable: the click both marks the
 * notification as read AND fires [onItemClick] so the host can deep-link the user
 * (e.g. open the user_challenge detail). "Đọc tất cả" wipes unread state without dismissing.
 */
@Composable
fun NotificationCenterSheet(
    notifications: List<NotificationEntity>,
    onItemClick: (NotificationEntity) -> Unit,
    onMarkAllRead: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(BetterMeColors.White)
                .navigationBarsPadding()
                .padding(20.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* swallow click */ },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thông báo",
                    style = BetterMeTypography.Title.Medium.Bold,
                    color = BetterMeColors.Text.TextPrimary
                )
                Text(
                    text = "Đọc tất cả",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Primary.Primary,
                    modifier = Modifier.clickable { onMarkAllRead() }
                )
            }
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có thông báo mới",
                        style = BetterMeTypography.Body.Medium,
                        color = BetterMeColors.Text.TextTertiary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications, key = { it.id }) { item ->
                        NotificationRow(item = item, onClick = { onItemClick(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    item: NotificationEntity,
    onClick: () -> Unit
) {
    val accent = when (item.type) {
        "CHALLENGE_REMINDER" -> BetterMeColors.Primary.Primary
        "CHALLENGE_START" -> BetterMeColors.Yellow
        "BADGE_AWARDED" -> BetterMeColors.Green
        else -> BetterMeColors.Text.TextSecondary
    }
    val emoji = when (item.type) {
        "CHALLENGE_REMINDER" -> "🔔"
        "CHALLENGE_START" -> "⏰"
        "BADGE_AWARDED" -> "🏅"
        else -> "📌"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (item.is_read) BetterMeColors.Gray.Gray3
                else BetterMeColors.Primary.PrimaryBackground
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.message,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatRelative(item.created_at),
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
            if (!item.is_read) {
                Spacer(modifier = Modifier.size(4.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(BetterMeColors.Red)
                )
            }
        }
    }
}

private fun formatRelative(ms: Long): String {
    val now = System.currentTimeMillis()
    val diffMin = ((now - ms) / 60_000L).coerceAtLeast(0)
    return when {
        diffMin < 1 -> "Vừa xong"
        diffMin < 60 -> "${diffMin} phút"
        diffMin < 60 * 24 -> "${diffMin / 60} giờ"
        else -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
    }
}

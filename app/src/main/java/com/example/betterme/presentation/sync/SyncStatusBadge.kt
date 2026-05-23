package com.example.betterme.presentation.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.betterme.domain.sync.SyncStatus
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight indicator pill that mirrors the four sync states:
 *  - SYNCING → "Đang đồng bộ…" (blue)
 *  - OFFLINE → "Ngoại tuyến" (gray)
 *  - ERROR → "Đồng bộ thất bại — bấm để thử lại" (red), tapping triggers retry
 *  - IDLE with lastSyncedAt → "Đã đồng bộ HH:mm" (green)
 *  - IDLE without lastSyncedAt → hidden (the app has never synced; no signal worth surfacing)
 *
 * Placed on the ChallengeOverview top bar by default so the user has a constant
 * visibility into sync health without dedicating a settings screen to it.
 *
 * Composable is hoisted: callers inject any [Modifier] for placement. The VM is
 * Koin-resolved internally so callers don't have to thread it.
 */
@Composable
fun SyncStatusBadge(
    modifier: Modifier = Modifier,
    viewModel: SyncStatusViewModel = koinViewModel()
) {
    val status by viewModel.status.collectAsState()
    val style = status.toBadgeStyle() ?: return

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(style.background)
            .let { base ->
                if (style.onTap != null) base.clickable { viewModel.syncNow() } else base
            }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = style.emoji, style = BetterMeTypography.Body.Small.Medium)
        Text(
            text = style.label,
            style = BetterMeTypography.Body.Small.Medium,
            color = style.foreground
        )
    }
}

private data class BadgeStyle(
    val emoji: String,
    val label: String,
    val foreground: Color,
    val background: Color,
    val onTap: (() -> Unit)? = null
)

private fun SyncStatus.toBadgeStyle(): BadgeStyle? {
    return when (state) {
        SyncStatus.State.SYNCING -> BadgeStyle(
            emoji = "🔄",
            label = "Đang đồng bộ…",
            foreground = BetterMeColors.Primary.Primary,
            background = BetterMeColors.Primary.Primary.copy(alpha = 0.10f)
        )
        SyncStatus.State.OFFLINE -> BadgeStyle(
            emoji = "📴",
            label = "Ngoại tuyến",
            foreground = BetterMeColors.Text.TextTertiary,
            background = BetterMeColors.Gray.Gray3
        )
        SyncStatus.State.ERROR -> BadgeStyle(
            emoji = "⚠️",
            label = "Đồng bộ thất bại — bấm để thử lại",
            foreground = BetterMeColors.Red,
            background = BetterMeColors.Red.copy(alpha = 0.10f),
            onTap = {} // marker — actual handler comes from the composable
        )
        SyncStatus.State.IDLE -> {
            val ts = lastSyncedAt ?: return null
            BadgeStyle(
                emoji = "✓",
                label = "Đã đồng bộ ${formatClock(ts)}",
                foreground = BetterMeColors.Green,
                background = BetterMeColors.Green.copy(alpha = 0.10f)
            )
        }
    }
}

private val clockFmt = SimpleDateFormat("HH:mm", Locale.forLanguageTag("vi"))
private fun formatClock(ms: Long): String = clockFmt.format(Date(ms))

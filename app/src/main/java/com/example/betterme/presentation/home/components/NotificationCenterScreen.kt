package com.example.betterme.presentation.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.R
import com.example.betterme.data.local.room.entities.NotificationEntity
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Full-screen notification inbox. Replaces the prior bottom-sheet — the inbox is
 * the primary read surface for habit-reminder + challenge-reminder + badge-award
 * notifications, so it deserves a proper screen with grouped history.
 *
 * Sections (oldest-first within each group):
 * - Hôm nay   — created since start-of-today
 * - Hôm qua   — created since start-of-yesterday, before start-of-today
 * - Cũ hơn   — older (kept up to 30 days by MidnightCleanupWorker)
 *
 * Each row:
 * - Emoji disc tinted by [NotificationEntity.type]
 * - Title (1 line), message (2 lines)
 * - Optional reminder time pill ("8:00 • Uống nước")
 * - Right column: relative timestamp + unread dot
 *
 * Tap:
 * - Marks-read (host invokes [onMarkRead]).
 * - If `habit_id != null` → [onHabitClick].
 * - Else if `user_challenge_id != null` or `challenge_id != null` → [onChallengeClick].
 *
 * The screen slides up from the bottom and fades the underlying Home behind a
 * scrim — matches the rest of the app's overlay grammar (CategoryDetail, etc.).
 */
@Composable
fun NotificationCenterScreen(
    notifications: List<NotificationEntity>,
    onMarkRead: (Int) -> Unit,
    onMarkAllRead: () -> Unit,
    onHabitClick: (Int) -> Unit,
    onChallengeClick: (userChallengeId: Int?, challengeId: Int?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Compute grouping once per recomposition. Stable references to the source list
    // mean the groups are only re-derived when the list itself changes.
    val grouped = remember(notifications) { groupByDay(notifications) }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(180)) + slideInVertically(
            initialOffsetY = { it / 6 },
            animationSpec = tween(220)
        ),
        exit = fadeOut(tween(160)) + slideOutVertically(
            targetOffsetY = { it / 6 },
            animationSpec = tween(160)
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(BetterMeColors.BackGround.BackgroundSecondary)
                .statusBarsPadding()
        ) {
            // Shared app top bar — consistent with Tasks / Habit Group / Add Habit.
            BetterMeTopBar(
                leadingIconRes = R.drawable.ic_arrow_left,
                title = "Thông báo",
                onLeadingClick = onClose
            )
            // Status row immediately under the top bar — preserves the unread
            // subtitle + "Đánh dấu tất cả đã đọc" action that previously lived
            // inside the custom header. Kept out of BetterMeTopBar so the top
            // bar can stay the canonical icon+title+icon shape used everywhere.
            InboxStatusRow(
                unreadCount = notifications.count { !it.is_read },
                hasAny = notifications.isNotEmpty(),
                onMarkAllRead = onMarkAllRead
            )

            if (notifications.isEmpty()) {
                EmptyState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    grouped.forEach { (label, rows) ->
                        item(key = "header-$label") {
                            SectionHeader(label = label, count = rows.size)
                        }
                        items(items = rows, key = { it.id }) { row ->
                            NotificationRow(
                                item = row,
                                onTap = {
                                    if (!row.is_read) onMarkRead(row.id)
                                    when {
                                        row.habit_id != null -> {
                                            onHabitClick(row.habit_id)
                                            onClose()
                                        }
                                        row.user_challenge_id != null ||
                                            row.challenge_id != null -> {
                                            onChallengeClick(row.user_challenge_id, row.challenge_id)
                                            onClose()
                                        }
                                        else -> Unit
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// STATUS ROW — sits under the shared top bar
// ============================================================
/**
 * Subtitle + mark-all-read action. Lives outside BetterMeTopBar so the top bar
 * itself stays the canonical icon-title-icon shape used everywhere else in the
 * app; the inbox-specific affordances live here without breaking that contract.
 */
@Composable
private fun InboxStatusRow(
    unreadCount: Int,
    hasAny: Boolean,
    onMarkAllRead: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (unreadCount > 0) "$unreadCount thông báo chưa đọc"
            else if (hasAny) "Tất cả đã đọc"
            else "Trống",
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary,
            modifier = Modifier.weight(1f)
        )
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                    .background(BetterMeColors.Primary.Primary.copy(alpha = 0.12f))
                    .clickable { onMarkAllRead() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Đánh dấu tất cả đã đọc",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Primary.Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ============================================================
// SECTION HEADER
// ============================================================
@Composable
private fun SectionHeader(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = BetterMeTypography.Title.Small.Bold,
            color = BetterMeColors.Text.TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(BetterMeColors.Primary.Primary.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$count",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Primary.Primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============================================================
// ROW
// ============================================================
@Composable
private fun NotificationRow(
    item: NotificationEntity,
    onTap: () -> Unit
) {
    val (accent, emoji) = decorationFor(item.type)
    val unreadSurface = accent.copy(alpha = 0.05f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (item.is_read) 1.dp else BetterMeTokens.CardElevation.Body,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Standard),
                ambientColor = BetterMeTokens.NeutralShadow.Ambient,
                spotColor = BetterMeTokens.NeutralShadow.Spot
            )
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Standard))
            .background(if (item.is_read) Color.White else unreadSurface)
            .border(
                width = if (item.is_read) 0.dp else 1.dp,
                color = if (item.is_read) Color.Transparent
                else accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Standard)
            )
            .clickable { onTap() }
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = if (item.is_read) FontWeight.Medium else FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.message.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.message,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Reminder time pill (e.g. "⏱ 08:00") if the row carries one.
            if (!item.reminder_time_label.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                        .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "⏱ ${item.reminder_time_label}",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatRelative(item.created_at),
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
            if (!item.is_read) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(BetterMeColors.Red)
                )
            }
        }
    }
}

// ============================================================
// EMPTY STATE
// ============================================================
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(BetterMeColors.Primary.PrimaryBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🔔", fontSize = 48.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Hộp thư trống",
            style = BetterMeTypography.Title.Medium.Bold,
            color = BetterMeColors.Text.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Khi đến giờ thói quen, thông báo của bạn sẽ xuất hiện ở đây để xem lại bất cứ lúc nào.",
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ============================================================
// HELPERS
// ============================================================

/**
 * Groups by Hôm nay / Hôm qua / Cũ hơn. Preserves the input order within each
 * group (the source list is already sorted DESC by created_at by the DAO).
 *
 * Returned as a list of (label, rows) pairs — a LinkedHashMap would re-order in
 * insertion order anyway, but a list makes the LazyColumn forEach trivially
 * idempotent and deterministic.
 */
private fun groupByDay(items: List<NotificationEntity>): List<Pair<String, List<NotificationEntity>>> {
    if (items.isEmpty()) return emptyList()
    val now = Calendar.getInstance()
    val startOfToday = now.startOfDayMillis()
    val startOfYesterday = startOfToday - DAY_MS

    val today = mutableListOf<NotificationEntity>()
    val yesterday = mutableListOf<NotificationEntity>()
    val older = mutableListOf<NotificationEntity>()

    for (it in items) {
        when {
            it.created_at >= startOfToday -> today += it
            it.created_at >= startOfYesterday -> yesterday += it
            else -> older += it
        }
    }
    return buildList {
        if (today.isNotEmpty()) add("Hôm nay" to today)
        if (yesterday.isNotEmpty()) add("Hôm qua" to yesterday)
        if (older.isNotEmpty()) add("Cũ hơn" to older)
    }
}

private fun Calendar.startOfDayMillis(): Long {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    return timeInMillis
}

private const val DAY_MS: Long = 24L * 60L * 60L * 1000L

private fun decorationFor(type: String): Pair<Color, String> = when (type) {
    "HABIT_REMINDER" -> BetterMeColors.Primary.Primary to "⏰"
    "CHALLENGE_REMINDER" -> BetterMeColors.Primary.Primary to "🔔"
    "CHALLENGE_START" -> BetterMeColors.Yellow to "🚀"
    "BADGE_AWARDED" -> BetterMeColors.Green to "🏅"
    else -> BetterMeColors.Text.TextSecondary to "📌"
}

private fun formatRelative(ms: Long): String {
    val now = System.currentTimeMillis()
    val diffSec = ((now - ms) / 1000L).coerceAtLeast(0L)
    val diffMin = diffSec / 60L
    val diffHour = diffMin / 60L
    val diffDay = diffHour / 24L
    return when {
        diffSec < 60 -> "Vừa xong"
        diffMin < 60 -> "$diffMin phút trước"
        diffHour < 24 -> "$diffHour giờ trước"
        diffDay < 7 -> "$diffDay ngày trước"
        else -> SimpleDateFormat("d/M", Locale.getDefault()).format(Date(ms))
    }
}

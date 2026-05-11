package com.example.betterme.presentation.statistics.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.statistics.ExpandedSection
import com.example.betterme.presentation.statistics.HabitStatusItem
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun HabitStatusSection(
    title: String,
    section: ExpandedSection,
    isExpanded: Boolean,
    habits: List<HabitStatusItem>,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onHabitClick: (Int) -> Unit = {}
) {
    val (accentColor, icon) = when (section) {
        ExpandedSection.COMPLETED -> Color(0xFF4CAF50) to Icons.Rounded.CheckCircle
        ExpandedSection.FAILED -> Color(0xFFE57373) to Icons.Rounded.Close
        ExpandedSection.ONGOING -> Color(0xFFFF9800) to Icons.Rounded.Refresh
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x08000000),
                spotColor = Color(0x14000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        // Header row (always visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "$title (${habits.size})",
                    style = BetterMeTypography.Title.Small.SemiBold,
                    color = BetterMeColors.Text.TextPrimary
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Thu gọn" else "Mở rộng",
                tint = BetterMeColors.Text.TextTertiary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Expandable content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                habits.forEach { habit ->
                    HabitStatusRow(
                        habit = habit,
                        accentColor = accentColor,
                        onClick = {
                            if (habit.habitId > 0) onHabitClick(habit.habitId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitStatusRow(
    habit: HabitStatusItem,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8F9FC))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = habit.icon,
                style = BetterMeTypography.Title.Small.Medium
            )
        }

        // Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = habit.name,
                style = BetterMeTypography.Title.Small.SemiBold,
                color = BetterMeColors.Text.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = habit.completionInfo,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { habit.percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accentColor,
                trackColor = Color(0xFFE8E8E8),
                strokeCap = StrokeCap.Round
            )
        }

        // Streak info / percentage
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${habit.percentage}%",
                style = BetterMeTypography.Title.Small.Bold,
                color = accentColor
            )
            Text(
                text = habit.streakInfo,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
    }
}

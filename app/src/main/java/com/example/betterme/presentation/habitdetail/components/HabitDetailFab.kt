package com.example.betterme.presentation.habitdetail.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Modern pill-shaped FAB for Habit Detail screen.
 *
 * Two visual states:
 * - **Not completed**: Camera check-in button (primary pill)
 * - **Completed today**: Undo + status row (two compact chips)
 */
@Composable
fun HabitDetailFab(
    isCompletedToday: Boolean,
    onCheckInClick: () -> Unit,
    onUndoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCompletedToday) {
            CompletedFabRow(onUndoClick = onUndoClick)
        } else {
            CheckInFab(onClick = onCheckInClick)
        }
    }
}

// ============================================================
// CHECK-IN FAB — Pill-shaped primary action button
// ============================================================
@Composable
private fun CheckInFab(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 10.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "fab_elevation"
    )

    Box(
        modifier = Modifier
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(28.dp),
                ambientColor = BetterMeColors.Primary.Primary.copy(alpha = 0.3f),
                spotColor = BetterMeColors.Primary.Primary.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(BetterMeColors.Primary.Primary)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "📸  Check in bằng camera",
            style = BetterMeTypography.Title.Small.Bold,
            color = BetterMeColors.White
        )
    }
}

// ============================================================
// COMPLETED FAB ROW — Undo + Status chips
// ============================================================
@Composable
private fun CompletedFabRow(
    onUndoClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Undo chip
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = BetterMeColors.Red.copy(alpha = 0.15f),
                    spotColor = BetterMeColors.Red.copy(alpha = 0.1f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(BetterMeColors.White)
                .clickable(onClick = onUndoClick)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Bỏ check-in",
                style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.SemiBold),
                color = BetterMeColors.Red
            )
        }

        // Completed status chip
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = BetterMeColors.Green.copy(alpha = 0.2f),
                    spotColor = BetterMeColors.Green.copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(BetterMeColors.Green)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓ Đã hoàn thành",
                style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.Bold),
                color = BetterMeColors.White
            )
        }
    }
}

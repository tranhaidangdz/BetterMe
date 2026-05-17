package com.example.betterme.presentation.leaderboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.betterme.domain.leaderboard.MotivationalEvent
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography
import kotlinx.coroutines.delay

/**
 * Slide-in motivational toast surfaced at the top of the leaderboard
 * screen when the latest snapshot produced an event.
 *
 * UX rules:
 *  - Auto-dismiss after [autoDismissMs] (default 3.5s).
 *  - Tap-to-dismiss.
 *  - One event at a time — the VM picks the highest-priority event;
 *    we don't queue or stack here.
 *
 * The composable is null-safe: when [event] is null the slot collapses
 * cleanly via AnimatedVisibility.
 */
@Composable
fun MotivationalEventToast(
    event: MotivationalEvent?,
    onDismiss: () -> Unit,
    autoDismissMs: Long = 3_500L,
    modifier: Modifier = Modifier
) {
    val isVisible = event != null
    val rememberedEvent = remember(event) { event }

    // Auto-dismiss timer — restarts every time a new event surfaces.
    LaunchedEffect(rememberedEvent) {
        if (rememberedEvent != null) {
            delay(autoDismissMs)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        if (rememberedEvent == null) return@AnimatedVisibility
        val accent = BetterMeColors.Primary.Primary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = BetterMeTokens.CardElevation.Body,
                    shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero),
                    ambientColor = BetterMeTokens.NeutralShadow.Ambient,
                    spotColor = BetterMeTokens.NeutralShadow.Spot
                )
                .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Hero))
                .background(accent.copy(alpha = 0.10f))
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                    shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero)
                )
                .clickable { onDismiss() }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = rememberedEvent.message,
                style = BetterMeTypography.Body.Medium,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

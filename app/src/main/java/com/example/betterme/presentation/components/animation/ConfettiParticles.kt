package com.example.betterme.presentation.components.animation

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.theme.BetterMeColors
import kotlin.random.Random

/**
 * 12 floating confetti dots that drift up/down with random delays. Used by both
 * the shared CheckInSuccessSheet and the challenge completion celebration dialog.
 *
 * Default colors are tuned for light backgrounds. Pass [colors] for dark themes
 * (e.g., the navy celebration modal).
 */
@Composable
fun ConfettiParticles(
    count: Int = 12,
    colors: List<Color> = listOf(
        BetterMeColors.Primary.Primary,
        BetterMeColors.Green,
        BetterMeColors.Red,
        BetterMeColors.Yellow,
        BetterMeColors.Primary.PrimaryBackground
    )
) {
    val particles = remember {
        List(count) {
            ConfettiData(
                offsetX = Random.nextFloat() * 80 - 40,
                offsetY = Random.nextFloat() * 80 - 40,
                size = Random.nextFloat() * 6 + 4,
                colorIndex = it % colors.size,
                delay = Random.nextInt(0, 400)
            )
        }
    }

    particles.forEach { particle ->
        val infiniteTransition = rememberInfiniteTransition(label = "confetti_${particle.delay}")

        val animY by infiniteTransition.animateFloat(
            initialValue = particle.offsetY,
            targetValue = particle.offsetY - 20,
            animationSpec = infiniteRepeatable(
                animation = tween(1200 + particle.delay, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "y_${particle.delay}"
        )

        val animAlpha by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 0.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000 + particle.delay),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha_${particle.delay}"
        )

        Box(
            modifier = Modifier
                .offset(x = particle.offsetX.dp, y = animY.dp)
                .size(particle.size.dp)
                .scale(animAlpha + 0.5f)
                .clip(CircleShape)
                .background(colors[particle.colorIndex].copy(alpha = animAlpha))
        )
    }
}

private data class ConfettiData(
    val offsetX: Float,
    val offsetY: Float,
    val size: Float,
    val colorIndex: Int,
    val delay: Int
)

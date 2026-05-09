package com.example.betterme.presentation.habitdetail.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Màn thành công sau check-in: animation + streak hiện tại
 */
@Composable
fun CheckInSuccessSheet(
    habitTitle: String,
    currentStreak: Int,
    onDismiss: () -> Unit,       // "Tuyệt vời 🔥"
    onViewHistory: () -> Unit,   // "Xem lịch sử"
    modifier: Modifier = Modifier
) {
    // ===== ENTRY ANIMATION =====
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BetterMeColors.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* block click through */ }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .clip(RoundedCornerShape(28.dp))
                .background(BetterMeColors.White)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ===== CONFETTI PARTICLES =====
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                // Floating confetti dots
                ConfettiParticles()

                // Main check icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(BetterMeColors.Green.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✅",
                        fontSize = 40.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== TITLE =====
            Text(
                text = "Check-in thành công!",
                style = BetterMeTypography.Title.Medium.Bold,
                color = BetterMeColors.Text.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ===== HABIT NAME =====
            Text(
                text = habitTitle,
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ===== STREAK BADGE =====
            if (currentStreak > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(BetterMeColors.Primary.PrimaryBackground)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🔥", fontSize = 24.sp)
                        Column {
                            Text(
                                text = "Chuỗi $currentStreak ngày",
                                style = BetterMeTypography.Title.Small.Bold,
                                color = BetterMeColors.Primary.Primary
                            )
                            Text(
                                text = "Tiếp tục phát huy!",
                                style = BetterMeTypography.Body.Small.Medium,
                                color = BetterMeColors.Primary.Primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ===== PRIMARY BUTTON =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BetterMeColors.Primary.Primary)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tuyệt vời 🔥",
                    style = BetterMeTypography.Title.Small.Bold,
                    color = BetterMeColors.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ===== SECONDARY BUTTON =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BetterMeColors.BackGround.BackgroundSecondary)
                    .clickable { onViewHistory() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Xem lịch sử",
                    style = BetterMeTypography.Body.Medium.copy(fontWeight = FontWeight.SemiBold),
                    color = BetterMeColors.Primary.Primary
                )
            }
        }
    }
}

// ============================================================
// CONFETTI ANIMATION — Các hạt bay xung quanh icon
// ============================================================
@Composable
private fun ConfettiParticles() {
    val colors = listOf(
        BetterMeColors.Primary.Primary,
        BetterMeColors.Green,
        BetterMeColors.Red,
        BetterMeColors.Yellow,
        BetterMeColors.Primary.PrimaryBackground
    )

    val particles = remember {
        List(12) {
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

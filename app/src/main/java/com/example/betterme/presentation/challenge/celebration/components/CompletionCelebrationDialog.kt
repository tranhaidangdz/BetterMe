package com.example.betterme.presentation.challenge.celebration.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.challenge.shared.ChipTheme
import com.example.betterme.presentation.challenge.shared.RewardChip
import com.example.betterme.presentation.components.animation.ConfettiParticles
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import kotlinx.coroutines.delay

/**
 * Full-screen celebration modal shown when a user completes a challenge.
 * Navy gradient bg, glowing trophy, confetti, share buttons, "Tuyệt vời!" CTA.
 */
@Composable
fun CompletionCelebrationDialog(
    challengeTitle: String,
    coinsEarned: Int,
    badgeName: String?,
    onShare: (SharePlatform) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80)
        visible = true
    }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.6f,
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
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* swallow click */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F1B33),
                            Color(0xFF1A2A4A)
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "✨ Chúc mừng! ✨",
                style = BetterMeTypography.Headline.Small.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Bạn đã hoàn thành thử thách",
                style = BetterMeTypography.Body.Medium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center
            ) {
                ConfettiParticles(
                    count = 16,
                    colors = listOf(
                        Color(0xFFFFD54F),
                        Color(0xFFFFA726),
                        Color(0xFFFFFFFF),
                        Color(0xFF42A5F5),
                        Color(0xFF66BB6A)
                    )
                )
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD54F).copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🏆", fontSize = 80.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = challengeTitle,
                style = BetterMeTypography.Title.Medium.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RewardChip(
                    coins = coinsEarned,
                    theme = ChipTheme.Dark
                )
                if (badgeName != null) {
                    RewardChip(
                        badgeName = badgeName,
                        theme = ChipTheme.Dark
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = "Chia sẻ thành tích",
                style = BetterMeTypography.Title.Small.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            ShareButtonRow(onShare = onShare)

            Spacer(modifier = Modifier.height(40.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tuyệt vời!",
                    style = BetterMeTypography.Title.Small.Bold,
                    color = BetterMeColors.Primary.Primary
                )
            }
        }
    }
}

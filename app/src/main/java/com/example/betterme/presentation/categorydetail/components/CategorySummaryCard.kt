package com.example.betterme.presentation.categorydetail.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.categorydetail.CategoryDetailState
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun CategorySummaryCard(
    state: CategoryDetailState,
    modifier: Modifier = Modifier
) {
    val accentColor = BetterMeColors.Primary.Primary
    val animatedPercent by animateFloatAsState(
        targetValue = state.groupCompletionPercent.toFloat(),
        animationSpec = tween(800),
        label = "progress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.10f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon + Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.categoryIcon,
                        style = BetterMeTypography.Headline.Small.Bold
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = state.categoryName,
                        style = BetterMeTypography.Title.Small.Bold,
                        color = BetterMeColors.Text.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${state.habits.size} thói quen · ${state.groupCompletionPercent}% hoàn thành",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
                Spacer(Modifier.height(8.dp))
                // Progress bar
                LinearProgressBar(
                    percent = animatedPercent / 100f,
                    color = accentColor,
                    height = 8.dp
                )
            }

            Spacer(Modifier.width(16.dp))

            // Circular progress
            CircularProgressLabel(
                percent = animatedPercent.toInt(),
                size = 72.dp,
                color = accentColor
            )
        }
    }
}

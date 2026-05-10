package com.example.betterme.presentation.challenge.badges.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.challenge.model.BadgeUiModel
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Single badge tile in [BadgeSection].
 *
 * Layout:
 * ```
 *   ┌──────┐
 *   │ PNG  │  artworkSize, ContentScale.Fit (no stretching)
 *   └──────┘
 *    Title   ← Title.Small.Bold, 1 line
 *  Criteria  ← Body.Small.Medium gray, up to 2 lines
 * ```
 * Earned badges render full-color. Locked badges are desaturated with [GrayscaleFilter]
 * and dimmed to 45% alpha — the desaturation IS the locked affordance, no extra "🔒" overlay.
 */
@Composable
fun BadgeGridItem(
    model: BadgeUiModel,
    modifier: Modifier = Modifier,
    tileWidth: Dp = 64.dp,
    artworkSize: Dp = 72.dp
) {
    Column(
        modifier = modifier.width(tileWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(artworkSize)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            if (model.iconRes != 0) {
                Image(
                    painter = painterResource(model.iconRes),
                    contentDescription = model.name,
                    contentScale = ContentScale.Fit,
                    colorFilter = if (model.isEarned) null else GrayscaleFilter,
                    modifier = Modifier
                        .size(artworkSize)
                        .alpha(if (model.isEarned) 1f else 0.45f)
                )
            } else {
                // Legacy fallback when no PNG drawable is configured for the badge.
                Box(
                    modifier = Modifier
                        .size(artworkSize)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (model.isEarned) model.accentColor.copy(alpha = 0.18f)
                            else BetterMeColors.Gray.Gray3
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (model.isEarned) model.iconEmoji else "🔒",
                        fontSize = 30.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = model.name,
            style = BetterMeTypography.Title.Small.Bold,
            color = if (model.isEarned) BetterMeColors.Text.TextPrimary
            else BetterMeColors.Text.TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = model.description,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontSize = 11.sp
        )
    }
}

/** Used to desaturate locked badge artwork. */
private val GrayscaleFilter: ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix().apply { setToSaturation(0f) }
)

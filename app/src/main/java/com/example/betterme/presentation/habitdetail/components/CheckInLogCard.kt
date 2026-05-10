package com.example.betterme.presentation.habitdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.betterme.presentation.habitdetail.CheckInLogUiModel
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Single row in the check-in history list. Renders a soft white card with:
 * - The optional check-in photo (loaded from a Cloudinary URL or any URI string saved
 *   on [CheckInLogUiModel.imageUri]). Hidden gracefully when no image was attached.
 *   Coil's memory + disk cache means each URL is fetched at most once even when the
 *   list scrolls or recomposes; failures fall back to a neutral placeholder rather
 *   than crashing.
 * - Date + status pill on top.
 * - Time + "Đã check in / Chưa check in" line.
 * - Optional note text.
 */
@Composable
fun CheckInLogCard(
    log: CheckInLogUiModel,
    modifier: Modifier = Modifier
) {
    val isDone = log.status == "DONE"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.03f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(BetterMeColors.White)
            .padding(14.dp)
    ) {
        // Header row: date + status pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = log.dateFormatted,
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isDone) BetterMeColors.Green.copy(alpha = 0.12f)
                        else BetterMeColors.Red.copy(alpha = 0.10f)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isDone) BetterMeColors.Green else BetterMeColors.Red)
                )
                Text(
                    text = if (isDone) "Hoàn thành" else "Bỏ lỡ",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = if (isDone) BetterMeColors.Green else BetterMeColors.Red,
                    maxLines = 1
                )
            }
        }

        // Photo (Cloudinary URL or local URI) — only rendered when an image was attached.
        if (!log.imageUri.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            CheckInPhoto(
                imageUri = log.imageUri,
                contentDescription = "Ảnh check-in ${log.dateFormatted}"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = log.timeFormatted,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
            Text(
                text = if (isDone) "Đã check in" else "Chưa check in",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextPrimary
            )
        }

        if (!log.note.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = log.note,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Wraps Coil's [AsyncImage] with the layout + caching defaults the history list expects:
 *
 * - Crossfade on first paint so images fade in instead of popping when the list scrolls
 *   back into view.
 * - Cache key is the URL itself (Coil default), so the in-memory + disk caches treat
 *   identical URLs as a single image — process death only forces one network round-trip.
 * - On error, the box collapses to a soft gray fill with a discreet "🖼" glyph; we never
 *   leave a blown-out red error icon in the history list.
 */
@Composable
private fun CheckInPhoto(
    imageUri: String,
    contentDescription: String
) {
    val context = LocalContext.current
    val request = remember(imageUri) {
        ImageRequest.Builder(context)
            .data(imageUri)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BetterMeColors.Gray.Gray3),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onError = { /* placeholder background already shows; nothing else to do */ }
        )
        // Fallback glyph painted underneath. AsyncImage stacks on top when it succeeds;
        // when it errors, the background + glyph remain visible.
        Text(
            text = "🖼",
            fontSize = 32.sp,
            color = BetterMeColors.Text.TextTertiary.copy(alpha = 0.4f)
        )
    }
}


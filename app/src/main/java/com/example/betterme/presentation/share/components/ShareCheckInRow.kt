package com.example.betterme.presentation.share.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.domain.share.VerifiedCheckIn
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single timeline row shared by the viewer + profile screens. Carries
 * the date formatter inline so callers don't need to wire a Locale
 * in.
 */
@Composable
fun ShareCheckInRow(
    row: VerifiedCheckIn,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = BetterMeColors.Border.BorderLight,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = if (row.kind == VerifiedCheckIn.Kind.CHALLENGE) "🏆" else "✅",
            fontSize = 20.sp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.name,
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = formatRowDate(row.date),
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
    }
}

private val rowDateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("vi"))
private fun formatRowDate(ms: Long): String = rowDateFmt.format(Date(ms))

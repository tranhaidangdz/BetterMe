package com.example.betterme.presentation.share.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * "Verified by Firebase" badge surfaced on the share viewer + public
 * profile screens. Two visual variants:
 *
 *  - [VerifiedFirebaseBadge] (default) — full-width pill suited to
 *    sitting just below the top bar. Round flame disc + bold caption.
 *  - [VerifiedFirebaseBadgeCompact] — inline chip used inside profile
 *    headers next to the display name.
 *
 * Palette mirrors Firebase's familiar amber/blue mix: amber disc with
 * a small "🔥" mark for the bird-like icon plus a deep-blue accent
 * for the text. The look is intentionally close to Strava / Duolingo
 * verification chips so it reads as "official" at a glance.
 */
@Composable
fun VerifiedFirebaseBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(BadgeBackground)
            .border(
                width = 1.dp,
                color = BadgeBorder,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Pill)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlameDisc()
        Spacer(Modifier.size(10.dp))
        Text(
            text = "Đã xác minh bởi Firebase",
            style = BetterMeTypography.Body.Small.Medium,
            color = BadgeForeground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "✓",
            color = BadgeAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

/** Inline chip variant — used inside profile headers + image cards. */
@Composable
fun VerifiedFirebaseBadgeCompact(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(BadgeBackground)
            .border(
                width = 1.dp,
                color = BadgeBorder,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Pill)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "🔥", fontSize = 12.sp)
        Spacer(Modifier.size(4.dp))
        Text(
            text = "Đã xác minh",
            style = BetterMeTypography.Body.Small.Medium,
            color = BadgeForeground,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FlameDisc() {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(BadgeAccent.copy(alpha = 0.18f))
            .border(width = 1.dp, color = BadgeAccent.copy(alpha = 0.36f), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "🔥", fontSize = 16.sp)
    }
}

// ─── Palette ───────────────────────────────────────────────────────
// Kept private and named so future surface variants (compact, dark-
// mode, image card) reuse the SAME hexes — no chance of drift.

private val BadgeBackground = Color(0xFFEFF5FF)  // soft Firebase-blue
private val BadgeBorder = Color(0xFFB6CCEC)
private val BadgeForeground = Color(0xFF1F3D6E) // deep blue text
private val BadgeAccent = Color(0xFFF59E0B)     // amber flame

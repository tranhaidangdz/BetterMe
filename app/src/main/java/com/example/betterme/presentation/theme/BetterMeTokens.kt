package com.example.betterme.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared visual tokens for the redesigned Home / Habit Group / Category Detail surfaces.
 *
 * Cards across these screens used to drift apart visually because each component baked
 * its own elevation, radius, and accent-opacity values inline. This object centralizes
 * the design language so every redesigned surface pulls from the same scale.
 *
 * Reference levels:
 * - [CardElevation] — `Body` for habit rows / category tiles, `Hero` for the top summary
 *   card on a screen. Difference is intentional: 4dp keeps body cards visibly lifted
 *   without making them feel like floating chips.
 * - [CardRadius]    — three-step rounded-corner scale (16/20/24). Larger radii reserved
 *   for the hero card so it reads as the primary element.
 * - [AccentAlpha]   — controlled tints derived from the accent color. Replaces the
 *   previously inconsistent 0.06f / 0.10f / 0.18f / 0.22f / 0.28f sprawl. Subtle,
 *   Soft, Medium, Strong only.
 * - [NeutralShadow] — neutral black shadow used by body cards. We switched away from
 *   the prior accent-tinted shadow (α 0.18–0.28) because that ran into chromatic
 *   conflict with the slightly-blue page background (#F2F8FF) and made cards read as
 *   color noise rather than lifted surfaces.
 *
 * Components are free to take the underlying value (dp / Color) directly; the names
 * here document intent.
 */
object BetterMeTokens {

    /** Vertical elevation. Cards do not stack; there are only two depths. */
    object CardElevation {
        val Body = 3.dp
        val Hero = 6.dp
    }

    /** Rounded-corner scale. Three steps only. */
    object CardRadius {
        val Pill = 999.dp
        val Body = 16.dp
        val Standard = 20.dp
        val Hero = 24.dp
    }

    /** Controlled tint scale applied as `accent.copy(alpha = X)`. */
    object AccentAlpha {
        const val Subtle = 0.06f       // barely-visible surface hint
        const val Soft = 0.10f          // pills, icon-disc backgrounds at rest
        const val Medium = 0.16f        // outline borders, emphasized pills
        const val Strong = 0.22f        // strong-state badge backgrounds (rare)
        const val DiscOuter = 0.22f     // outer ring of the tinted emoji discs
        const val DiscInner = 0.10f     // inner of the tinted emoji discs
    }

    /** Neutral shadow used by body cards. Replaces the prior accent-tinted shadow
     *  which clashed with the pale-blue page background. */
    object NeutralShadow {
        val Ambient = Color(0x14000000)
        val Spot = Color(0x1F000000)
    }
}

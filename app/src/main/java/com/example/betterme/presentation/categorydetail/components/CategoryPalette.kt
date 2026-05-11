package com.example.betterme.presentation.categorydetail.components

import androidx.compose.ui.graphics.Color

/**
 * Per-category visual identity used across Home category sections, Tasks tab and the
 * Habit Group / Category Detail screen.
 *
 * Why these specific colors:
 * - All accents are picked from Tailwind's *-600 weight band. The prior palette mixed
 *   -500 and -700 values (orange-500 / slate-700) which made the categories feel
 *   randomly saturated. Pulling them all from a single band means orange, green,
 *   violet, indigo, slate, and pink read as a coordinated set rather than six
 *   independent picks.
 * - The companion `soft` color is no longer a separate Tailwind -100 pastel. Each
 *   `soft` is the same accent at AccentAlpha.Soft (0.10) over white, computed once
 *   here. Holding the math in the palette keeps surface tints in lockstep with the
 *   accent — no chance of an orange row gaining a green pill if a future ref drifts.
 * - The motivational subtitle is part of the identity tuple, so the visual + verbal
 *   identity always arrive together when a component pulls `paletteFor(categoryId)`.
 *
 * Falls back to a neutral indigo palette for any id not in the map (also Tailwind 600).
 */
data class CategoryPalette(
    /** Strong tone used for progress bars, percent text, chevrons, primary CTAs. */
    val accent: Color,
    /** Same accent at low alpha — used for soft surface tints + chip backgrounds. */
    val soft: Color,
    /** One-line motivational tagline shown beneath the category header. */
    val subtitle: String
)

private fun palette(accent: Color, subtitle: String) = CategoryPalette(
    accent = accent,
    // Soft = accent at 10% over white. Lock-step with the accent — never an ad-hoc
    // pastel that could drift from the main color identity.
    soft = accent.copy(alpha = 0.10f),
    subtitle = subtitle
)

private val FALLBACK = palette(
    accent = Color(0xFF4F46E5),
    subtitle = "Mỗi thói quen nhỏ là một bước tiến lớn."
)

private val PALETTES: Map<Int, CategoryPalette> = mapOf(
    // 1 — Vận động & thể chất → Fitness (orange-600)
    1 to palette(
        accent = Color(0xFFEA580C),
        subtitle = "Cơ thể khoẻ mạnh là nền tảng của mọi điều khác."
    ),
    // 2 — Dinh dưỡng → Nutrition (green-600)
    2 to palette(
        accent = Color(0xFF16A34A),
        subtitle = "Bạn là những gì bạn ăn — hãy chọn lành mạnh."
    ),
    // 3 — Tinh thần → Mindfulness (violet-600)
    3 to palette(
        accent = Color(0xFF7C3AED),
        subtitle = "Tâm trí bình lặng là siêu năng lực thực sự."
    ),
    // 4 — Học tập → Learning (indigo-600)
    4 to palette(
        accent = Color(0xFF4F46E5),
        subtitle = "Mỗi ngày một chút — kiến thức là khoản lãi kép."
    ),
    // 5 — Kỷ luật → Discipline (slate-700 — kept slightly deeper than -600 so the
    //                            "premium / serious" tone reads correctly)
    5 to palette(
        accent = Color(0xFF334155),
        subtitle = "Kỷ luật là cây cầu giữa mục tiêu và thành công."
    ),
    // 6 — Mối quan hệ → Relationships (pink-600)
    6 to palette(
        accent = Color(0xFFDB2777),
        subtitle = "Những người quan trọng xứng đáng nhận được thời gian của bạn."
    )
)

fun paletteFor(categoryId: Int): CategoryPalette = PALETTES[categoryId] ?: FALLBACK

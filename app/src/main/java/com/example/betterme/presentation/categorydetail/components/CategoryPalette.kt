package com.example.betterme.presentation.categorydetail.components

import androidx.compose.ui.graphics.Color

/**
 * Per-category visual identity used across the Habit Group / Category Detail screen.
 *
 * Each [CategoryPalette] carries:
 * - [accent]   — strong color used for percentage pills, progress bars, accent text.
 * - [soft]     — same hue at reduced saturation; used as the gradient highlight inside
 *                white surfaces (matches the HomeCard / CantMissCard design system).
 * - [subtitle] — a short motivational line shown beneath the group header so each
 *                category feels intentional rather than generic.
 *
 * Mapping mirrors the seeded category IDs (fakeCategories order). Falls back to a
 * neutral blue palette for any id not in the map.
 */
data class CategoryPalette(
    val accent: Color,
    val soft: Color,
    val subtitle: String
)

private val FALLBACK = CategoryPalette(
    accent = Color(0xFF3B82F6),
    soft = Color(0xFFDBEAFE),
    subtitle = "Mỗi thói quen nhỏ là một bước tiến lớn."
)

private val PALETTES: Map<Int, CategoryPalette> = mapOf(
    // 1 — Vận động & thể chất → Fitness: energetic orange/red
    1 to CategoryPalette(
        accent = Color(0xFFF97316),
        soft = Color(0xFFFFEDD5),
        subtitle = "Cơ thể khoẻ mạnh là nền tảng của mọi điều khác."
    ),
    // 2 — Dinh dưỡng → Health/Nutrition: fresh green
    2 to CategoryPalette(
        accent = Color(0xFF22C55E),
        soft = Color(0xFFDCFCE7),
        subtitle = "Bạn là những gì bạn ăn — hãy chọn lành mạnh."
    ),
    // 3 — Tinh thần → Mindfulness: calm purple
    3 to CategoryPalette(
        accent = Color(0xFF8B5CF6),
        soft = Color(0xFFEDE9FE),
        subtitle = "Tâm trí bình lặng là siêu năng lực thực sự."
    ),
    // 4 — Học tập → Learning: clean indigo
    4 to CategoryPalette(
        accent = Color(0xFF6366F1),
        soft = Color(0xFFE0E7FF),
        subtitle = "Mỗi ngày một chút — kiến thức là khoản lãi kép."
    ),
    // 5 — Kỷ luật → Discipline: premium slate
    5 to CategoryPalette(
        accent = Color(0xFF475569),
        soft = Color(0xFFE2E8F0),
        subtitle = "Kỷ luật là cây cầu giữa mục tiêu và thành công."
    ),
    // 6 — Mối quan hệ → Relationships: warm pink/coral
    6 to CategoryPalette(
        accent = Color(0xFFEC4899),
        soft = Color(0xFFFCE7F3),
        subtitle = "Những người quan trọng xứng đáng nhận được thời gian của bạn."
    )
)

fun paletteFor(categoryId: Int): CategoryPalette = PALETTES[categoryId] ?: FALLBACK

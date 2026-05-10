package com.example.betterme.presentation.challenge.shared

import androidx.compose.ui.graphics.Color

/**
 * Difficulty tiers for challenges.
 *
 * - EASY      → 7-day starter challenges
 * - MEDIUM    → 14-day momentum builders
 * - HARD      → 30-day deep commitment
 * - LEGENDARY → 60–90-day transformations (Monk Mode tier)
 */
enum class Difficulty(val raw: String, val label: String, val color: Color, val bg: Color) {
    EASY("EASY", "Dễ", Color(0xFF10B981), Color(0xFFE8F5E9)),
    MEDIUM("MEDIUM", "Trung bình", Color(0xFFF59E0B), Color(0xFFFFF3E0)),
    HARD("HARD", "Khó", Color(0xFFEF4444), Color(0xFFFFEBEE)),
    LEGENDARY("LEGENDARY", "Huyền thoại", Color(0xFF7C3AED), Color(0xFFF3E8FF));

    companion object {
        fun fromRaw(value: String?): Difficulty = when (value) {
            "MEDIUM" -> MEDIUM
            "HARD" -> HARD
            "LEGENDARY" -> LEGENDARY
            else -> EASY
        }
    }
}

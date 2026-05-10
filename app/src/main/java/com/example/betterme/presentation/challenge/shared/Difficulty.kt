package com.example.betterme.presentation.challenge.shared

import androidx.compose.ui.graphics.Color

enum class Difficulty(val raw: String, val label: String, val color: Color, val bg: Color) {
    EASY("EASY", "Dễ", Color(0xFF10B981), Color(0xFFE8F5E9)),
    MEDIUM("MEDIUM", "Trung bình", Color(0xFFF59E0B), Color(0xFFFFF3E0)),
    HARD("HARD", "Khó", Color(0xFFEF4444), Color(0xFFFFEBEE));

    companion object {
        fun fromRaw(value: String?): Difficulty = when (value) {
            "MEDIUM" -> MEDIUM
            "HARD" -> HARD
            else -> EASY
        }
    }
}

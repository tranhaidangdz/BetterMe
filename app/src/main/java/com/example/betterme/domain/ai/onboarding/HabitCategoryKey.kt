package com.example.betterme.domain.ai.onboarding

/**
 * The 8-value taxonomy the AI onboarding suggester emits. Decoupled from
 * BetterMe's actual [com.example.betterme.data.local.room.entities.CategoryEntity]
 * seed because:
 *
 *  - The prompt uses short English enum values (cleaner for the LLM).
 *  - The DB ships only ~6 Vietnamese-labelled categories ("Vận động & thể chất",
 *    "Tinh thần & sức khỏe tâm lý", etc.) that don't 1:1 map onto the enum.
 *
 * Each enum value carries [matchKeywords] — substrings of Vietnamese category
 * names — that the use case scans against the actual `CategoryRepository`
 * output to resolve a real category id. First non-null keyword match wins;
 * no match → habit lands without a category (still valid in `HabitEntity`).
 *
 * Adding a new BetterMe category later only requires extending [matchKeywords]
 * here; the prompt stays untouched.
 */
enum class HabitCategoryKey(val matchKeywords: List<String>) {
    FITNESS(listOf("Vận động", "thể chất")),
    HEALTH(listOf("Dinh dưỡng", "Sức khỏe", "ăn uống")),
    PRODUCTIVITY(listOf("Năng suất", "Kỷ luật", "Sinh hoạt")),
    STUDY(listOf("Học tập", "phát triển")),
    SLEEP(listOf("Sinh hoạt", "Kỷ luật")),
    MINDFULNESS(listOf("Tinh thần", "tâm lý")),
    SELF_CARE(listOf("Chăm sóc", "Tinh thần", "tâm lý")),
    DISCIPLINE(listOf("Kỷ luật", "Sinh hoạt"));

    companion object {
        fun fromStringOrNull(raw: String?): HabitCategoryKey? {
            val cleaned = raw?.trim()?.uppercase() ?: return null
            return runCatching { valueOf(cleaned) }.getOrNull()
        }
    }
}

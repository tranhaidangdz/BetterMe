package com.example.betterme.domain.ai.habitcreation

/**
 * Pre-save analysis returned by the AI Habit Creation Assistant.
 *
 * The assistant is **advisory only** — it never blocks the user from creating
 * a habit. `shouldWarn = true` just tells the UI to show the bottom sheet;
 * the user always retains the final "Vẫn tạo" / "Áp dụng gợi ý" choice.
 *
 * [isCanned] = true when every OpenRouter model in the fallback chain failed
 * and the repo served a rule-based local analysis. Use cases skip caching
 * canned content so the next save attempt is free to produce a real one.
 */
data class HabitCreationAnalysis(
    val shouldWarn: Boolean,
    val overallRisk: CreationRiskLevel,
    val warnings: List<HabitCreationWarning>,
    val suggestions: List<HabitCreationSuggestion>,
    /** ≤ 1-sentence Vietnamese supportive line. */
    val encouragement: String,
    val isCanned: Boolean = false
)

enum class CreationRiskLevel { LOW, MODERATE, HIGH }

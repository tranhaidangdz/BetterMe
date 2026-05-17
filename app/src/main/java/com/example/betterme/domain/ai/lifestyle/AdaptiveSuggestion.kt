package com.example.betterme.domain.ai.lifestyle

/**
 * One actionable coaching suggestion. The model emits 1–4 per analysis.
 *
 * Semantics of each [type] (kept here so the prompt and the use case can't
 * drift on what each enum value actually means):
 *
 *  - [SuggestionType.REDUCE_INTENSITY]   — lower the difficulty of existing habits
 *  - [SuggestionType.SIMPLIFY_ROUTINE]   — cut the total habit count
 *  - [SuggestionType.IMPROVE_SLEEP]      — prioritize sleep stabilization
 *  - [SuggestionType.REDUCE_OVERLOAD]    — spread habits across more time windows
 *  - [SuggestionType.ADD_RECOVERY]       — insert restorative habits / breaks
 *  - [SuggestionType.IMPROVE_CONSISTENCY] — smaller, more frequent commitment
 *  - [SuggestionType.MAINTAIN_STABILITY] — "keep going, you're doing fine"
 *
 * All three string fields are 1-sentence Vietnamese (the prompt enforces).
 */
data class AdaptiveSuggestion(
    val type: SuggestionType,
    val title: String,
    val reason: String,
    val suggestion: String
)

enum class SuggestionType {
    REDUCE_INTENSITY,
    SIMPLIFY_ROUTINE,
    IMPROVE_SLEEP,
    REDUCE_OVERLOAD,
    ADD_RECOVERY,
    IMPROVE_CONSISTENCY,
    MAINTAIN_STABILITY
}

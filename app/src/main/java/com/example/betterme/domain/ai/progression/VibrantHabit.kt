package com.example.betterme.domain.ai.progression

/**
 * One well-performing habit the progression engine flags as "ready to
 * grow". Mirror of [com.example.betterme.domain.ai.recovery.StrugglingHabit]
 * but inverted — every value here represents a positive signal.
 *
 * The card renders these in the "Thói quen đang vững" block so the user
 * can see *which* habits earned the progression suggestion — anchoring
 * the advice in their own data rather than abstract praise.
 */
data class VibrantHabit(
    val title: String,
    val completionRate7d: Int,
    val completionRate14d: Int,
    /** Current consecutive-done streak in days. */
    val currentStreak: Int,
    /** Short Vietnamese line the AI / canned fallback writes — e.g.
     *  "Giữ vững 2 tuần liền — nền tảng đã đủ chắc cho bước tiếp theo." */
    val readinessReason: String
)

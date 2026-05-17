package com.example.betterme.domain.ai.personalization

/**
 * Coarse-grained classification of a habit category by intent. Drives:
 *  - The category-specific cues fed to the system prompt (fitness =
 *    detect overtraining; study = detect cognitive overload; etc.).
 *  - The canned template pool keying, so offline coaching for the
 *    Fitness group reads nothing like the Sleep group's even though
 *    both fall back to canned content.
 *
 * We classify by Vietnamese keywords against [CategoryEntity.name] —
 * BetterMe doesn't ship a `type` column on the category table and adding
 * one would force a schema migration the spec explicitly forbids.
 * Substring matching is good enough because the seeded category catalog
 * has stable, descriptive names ("Vận động & thể chất", "Học tập", etc.).
 *
 * [OTHER] is the safe fallback — the system prompt + canned pool both
 * have generic content for it that doesn't pretend to know the category's
 * nature. Better than silently mis-classifying ("Tài chính" as Fitness
 * because "chính" contains an "i").
 */
enum class CategoryKind(
    /** Vietnamese coaching tag the system prompt embeds. */
    val coachingTag: String
) {
    FITNESS("vận động / thể chất"),
    STUDY("học tập / phát triển trí tuệ"),
    SLEEP("nghỉ ngơi / giấc ngủ"),
    MINDFULNESS("tinh thần / chánh niệm"),
    NUTRITION("dinh dưỡng / ăn uống"),
    FINANCE("tài chính / chi tiêu"),
    RELATIONSHIPS("quan hệ / kết nối"),
    OTHER("đời sống chung");

    companion object {

        /**
         * Map a category display name to a [CategoryKind]. Case-insensitive
         * substring match against the keyword set for each kind, first-hit
         * wins. Names not matching any keyword set fall through to [OTHER].
         */
        fun classify(categoryName: String?): CategoryKind {
            if (categoryName.isNullOrBlank()) return OTHER
            val lower = categoryName.lowercase()
            return when {
                KEYWORDS_FITNESS.any { lower.contains(it) } -> FITNESS
                KEYWORDS_SLEEP.any { lower.contains(it) } -> SLEEP
                KEYWORDS_MINDFULNESS.any { lower.contains(it) } -> MINDFULNESS
                KEYWORDS_NUTRITION.any { lower.contains(it) } -> NUTRITION
                KEYWORDS_FINANCE.any { lower.contains(it) } -> FINANCE
                KEYWORDS_RELATIONSHIPS.any { lower.contains(it) } -> RELATIONSHIPS
                // Study comes last in the keyword chain — "học" appears
                // inside several other Vietnamese terms ("học cách thở")
                // and we'd rather classify by their *primary* category.
                KEYWORDS_STUDY.any { lower.contains(it) } -> STUDY
                else -> OTHER
            }
        }

        private val KEYWORDS_FITNESS = listOf("vận động", "thể chất", "thể thao", "tập luyện", "fitness", "tập gym", "chạy")
        private val KEYWORDS_STUDY = listOf("học tập", "học", "đọc sách", "kỹ năng", "trí tuệ", "kiến thức")
        private val KEYWORDS_SLEEP = listOf("ngủ", "nghỉ ngơi", "giấc ngủ")
        private val KEYWORDS_MINDFULNESS = listOf("tinh thần", "thiền", "chánh niệm", "tâm trí", "cảm xúc", "sức khỏe tinh thần", "sức khoẻ tinh thần")
        private val KEYWORDS_NUTRITION = listOf("dinh dưỡng", "ăn uống", "uống nước", "bữa ăn", "thực phẩm")
        private val KEYWORDS_FINANCE = listOf("tài chính", "chi tiêu", "tiết kiệm", "đầu tư", "ngân sách")
        private val KEYWORDS_RELATIONSHIPS = listOf("quan hệ", "gia đình", "bạn bè", "kết nối", "xã hội")
    }
}

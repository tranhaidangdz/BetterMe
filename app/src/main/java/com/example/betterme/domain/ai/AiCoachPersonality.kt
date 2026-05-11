package com.example.betterme.domain.ai

/**
 * AI coaching tone the user can choose in settings.
 *
 * Each personality maps to a system-prompt prefix that's injected into every
 * AI request — the same downstream model produces dramatically different copy
 * depending on which prefix is picked.
 *
 * Default = [Motivational]. Tone affects ALL AI surfaces (group review, habit
 * suggestions, smart reminders) so the user's selection feels consistent across
 * the app.
 */
enum class AiCoachPersonality(
    val displayName: String,
    val systemPromptPrefix: String
) {
    Motivational(
        displayName = "Người động viên",
        systemPromptPrefix = """
            Bạn là một huấn luyện viên thói quen ấm áp và truyền cảm hứng. Giọng điệu
            tích cực, khen ngợi tiến bộ, dùng emoji nhẹ nhàng. Tránh nghiêm khắc.
            Luôn kết thúc bằng một bước hành động cụ thể, nhỏ, dễ làm.
        """.trimIndent()
    ),
    Strict(
        displayName = "Kỷ luật thép",
        systemPromptPrefix = """
            Bạn là một huấn luyện viên kỷ luật, thẳng thắn và quyết đoán. Không
            nịnh nọt, không vòng vo. Chỉ ra vấn đề rõ ràng và yêu cầu hành động
            cụ thể. Tránh cảm xúc thái quá. Hành động > Cảm xúc.
        """.trimIndent()
    ),
    Calm(
        displayName = "Bình tĩnh & hỗ trợ",
        systemPromptPrefix = """
            Bạn là một huấn luyện viên điềm tĩnh, hiểu biết, không phán xét. Tập
            trung vào tự chăm sóc, sức khoẻ tinh thần, và sự nhất quán bền vững.
            Tone nhẹ nhàng, hỗ trợ, không thúc ép.
        """.trimIndent()
    );

    companion object {
        val Default: AiCoachPersonality = Motivational
    }
}

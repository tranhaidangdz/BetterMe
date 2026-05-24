package com.example.betterme.domain.ai

/**
 * Machine-readable categorization of why an AI call failed. Carried inside
 * [AiUnavailableException] so the ViewModel layer can render a category-appropriate
 * Vietnamese error message instead of a generic "Không thể phân tích lúc này".
 *
 * Categories are mutually exclusive: each request maps to exactly one category.
 * The mapping rules live in `AiHabitInsightRepositoryImpl.categorize(...)`.
 */
enum class AiErrorCategory {
    /** Device has no internet (DNS / IOException). User should connect WiFi or mobile data. */
    NO_NETWORK,

    /** HTTP 401 — `OPENROUTER_API_KEY` is missing / wrong / revoked. Build-time issue. */
    INVALID_KEY,

    /** HTTP 402 (payment required) — account credit exhausted. */
    QUOTA_EXCEEDED,

    /**
     * HTTP 429 sustained across every model in the chain. Different from "one model
     * throttled and we fell through" — this is "every fallback also 429'd".
     */
    RATE_LIMITED,

    /**
     * HTTP 404 / 400 — the requested model is unknown to OpenRouter or doesn't accept
     * our request shape. Most common cause: the model name in [FALLBACK_MODELS] was
     * deprecated and removed from the free tier; the fix is to refresh the list.
     */
    MODEL_UNAVAILABLE,

    /** SocketTimeoutException after one retry — free models on cold start are slow. */
    TIMEOUT,

    /** Response body wasn't valid JSON / didn't match the expected schema. */
    PARSE,

    /** HTTP 5xx — OpenRouter / upstream provider is down. */
    SERVER_ERROR,

    /** Anything else (genuinely unexpected). */
    UNKNOWN
}

/**
 * Thrown by the AI repo when every model in the OpenRouter fallback chain fails.
 *
 * [category] is the machine-readable failure bucket; [message] is a Vietnamese
 * user-facing string. ViewModels prefer category-aware UI but can fall through to
 * the message for any state they don't explicitly handle.
 */
class AiUnavailableException(
    val category: AiErrorCategory,
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    companion object {
        /**
         * Vietnamese user-facing copy keyed by category. Centralized so every VM
         * shows the same string for the same root cause.
         */
        fun userMessage(category: AiErrorCategory, fallback: String? = null): String = when (category) {
            AiErrorCategory.NO_NETWORK ->
                "Không có kết nối mạng. Hãy bật Wi-Fi hoặc dữ liệu di động rồi thử lại."
            AiErrorCategory.INVALID_KEY ->
                "Khóa AI không hợp lệ. Vui lòng liên hệ nhà phát triển."
            AiErrorCategory.QUOTA_EXCEEDED ->
                "Hạn mức AI đã hết. Hãy thử lại sau hoặc nâng cấp tài khoản OpenRouter."
            AiErrorCategory.RATE_LIMITED ->
                "AI đang quá tải. Vui lòng đợi vài phút rồi thử lại."
            AiErrorCategory.MODEL_UNAVAILABLE ->
                "Mô hình AI hiện không khả dụng. Ứng dụng sẽ thử lại sau khi cập nhật."
            AiErrorCategory.TIMEOUT ->
                "AI phản hồi quá chậm. Hãy thử lại — kết nối có thể đã ổn định hơn."
            AiErrorCategory.PARSE ->
                "AI trả về dữ liệu không hợp lệ. Hãy thử lại."
            AiErrorCategory.SERVER_ERROR ->
                "Máy chủ AI đang gặp sự cố. Hãy thử lại sau ít phút."
            AiErrorCategory.UNKNOWN ->
                fallback ?: "Đã xảy ra lỗi không xác định khi gọi AI. Hãy thử lại."
        }
    }
}

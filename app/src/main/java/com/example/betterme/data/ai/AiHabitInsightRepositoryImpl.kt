package com.example.betterme.data.ai

import android.util.Log
import com.example.betterme.data.ai.dto.ChatMessage
import com.example.betterme.data.ai.dto.ChatRequest
import com.example.betterme.domain.ai.AiCoachPersonality
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.ai.AiHabitInsightRepository.AiResult
import com.example.betterme.domain.ai.AiHabitInsightRepository.AiSuggestResult
import com.example.betterme.domain.ai.SuggestedHabit
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * OpenRouter-backed implementation.
 *
 * Model chain (tried in order)
 * 1. google/gemini-2.5-flash-preview:free  — fastest free model, strongest JSON.
 * 2. google/gemini-2.0-flash-exp:free      — previous-gen Gemini, separate quota.
 * 3. meta-llama/llama-3.3-70b-instruct:free — strong reasoning, slower.
 * 4. mistralai/mistral-small-3.1-24b-instruct:free — last-line free option.
 *
 * Per-model behaviour
 * - One retry on 429/500/502/503/504 with 800ms backoff. Anything else fails
 *   fast — retrying 401/403/404 just burns time.
 * - On any failure, the chain advances to the next model. The user never sees
 *   intermediate failures unless EVERY model is exhausted.
 *
 * Last-resort fallback
 * - If all 4 models fail, the repo returns a canned (handwritten) response
 *   tagged with `isCanned = true`. The use cases never cache canned content,
 *   so the next attempt is fresh. The UI renders canned content through the
 *   same premium card — the user always sees meaningful content.
 *
 * Output shaping
 * - max_tokens = 120 across the board. Prompts are deliberately terse so a
 *   120-token reply still feels complete. This trades occasional truncation
 *   (caught by the canned fallback) for a much smaller per-call quota burn.
 */
class AiHabitInsightRepositoryImpl(
    private val api: OpenRouterApi
) : AiHabitInsightRepository {

    // ============================================================
    // REVIEW
    // ============================================================
    override suspend fun reviewHabitGroup(
        categoryName: String,
        stats: String,
        personality: AiCoachPersonality
    ): AiResult {
        val systemPrompt = personality.systemPromptPrefix +
            "\nTrả lời tiếng Việt, 3-4 câu, súc tích, chỉ dùng số có trong dữ liệu."
        val userPrompt = "Nhóm: \"$categoryName\".\n$stats"

        val messages = listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = userPrompt)
        )

        var lastFailure: AiResult.Failure? = null
        for ((index, model) in FALLBACK_MODELS.withIndex()) {
            val result = retryOnTransient { tryModel(model, messages) }
            if (result is AiResult.Success) return result
            lastFailure = result as AiResult.Failure
            Log.w(TAG, "Review model[$index]=$model failed: ${lastFailure.message}")
        }

        Log.w(TAG, "All review models exhausted — serving canned review")
        return AiResult.Success(text = cannedReview(categoryName), isCanned = true)
    }

    // ============================================================
    // SUGGESTIONS
    // ============================================================
    override suspend fun suggestHabits(
        categoryName: String,
        existingHabitTitles: List<String>,
        personality: AiCoachPersonality
    ): AiSuggestResult {
        val existingList = if (existingHabitTitles.isEmpty()) "không có"
        else existingHabitTitles.joinToString("; ")

        // Compact JSON-only prompt. No markdown rules, no "BẮT BUỘC" filler — the
        // model is told the exact schema once and asked to fill it.
        val systemPrompt =
            """
            Trả về JSON: {"suggestions":[{"title":"","emoji":"","description":"","difficulty":"EASY|MEDIUM|HARD","estimatedImpact":"","streakBenefit":""}]}
            3-4 mục, tiếng Việt rất ngắn, không trùng thói quen đã có. Không kèm chữ ngoài JSON.
            """.trimIndent()
        val userPrompt = "Nhóm: \"$categoryName\". Đã có: $existingList."

        val messages = listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = userPrompt)
        )

        var lastFailure: AiSuggestResult.Failure? = null
        for ((index, model) in FALLBACK_MODELS.withIndex()) {
            val result = retryOnTransient { tryModelJson(model, messages) }
            if (result is AiSuggestResult.Success) return result
            lastFailure = result as AiSuggestResult.Failure
            Log.w(TAG, "Suggest model[$index]=$model failed: ${lastFailure.message}")
        }

        Log.w(TAG, "All suggest models exhausted — serving canned suggestions")
        return AiSuggestResult.Success(suggestions = cannedSuggestions(), isCanned = true)
    }

    // ============================================================
    // RETRY POLICY
    // ============================================================
    /**
     * One retry on transient failures only. 429/500/502/503/504 and read timeouts
     * are worth a second swing — auth, payment, and "model not found" are not.
     */
    private suspend fun <T> retryOnTransient(block: suspend () -> T): T {
        val first = block()
        if (!isTransientFailure(first)) return first
        Log.d(TAG, "Transient failure — retrying once after ${RETRY_DELAY_MS}ms backoff")
        delay(RETRY_DELAY_MS)
        return block()
    }

    private fun isTransientFailure(result: Any?): Boolean = when (result) {
        is AiResult.Failure -> result.message.containsTransientCode()
        is AiSuggestResult.Failure -> result.message.containsTransientCode()
        else -> false
    }

    private fun String.containsTransientCode(): Boolean =
        contains("(429)") || contains("(500)") || contains("(502)") ||
            contains("(503)") || contains("(504)") || contains("Mạng chậm")

    // ============================================================
    // HTTP — REVIEW (plain text)
    // ============================================================
    private suspend fun tryModel(
        model: String,
        messages: List<ChatMessage>
    ): AiResult {
        Log.d(TAG, "Using model=$model")
        return try {
            val response = api.chatCompletion(
                ChatRequest(
                    model = model,
                    messages = messages,
                    maxTokens = MAX_TOKENS,
                    temperature = TEMPERATURE
                )
            )
            if (response.error != null) {
                return AiResult.Failure(
                    response.error.message ?: "Yêu cầu AI bị từ chối"
                )
            }
            val content = response.choices.firstOrNull()?.message?.content?.trim()
            if (content.isNullOrBlank()) {
                AiResult.Failure("AI không trả lời — vui lòng thử lại")
            } else {
                AiResult.Success(content)
            }
        } catch (e: java.net.SocketTimeoutException) {
            AiResult.Failure("Mạng chậm — AI hết thời gian chờ")
        } catch (e: HttpException) {
            AiResult.Failure(extractHttpErrorMessage(e))
        } catch (e: java.io.IOException) {
            AiResult.Failure("Không thể kết nối đến AI — kiểm tra mạng")
        } catch (e: Exception) {
            Log.e(TAG, "AI request threw", e)
            AiResult.Failure("Lỗi AI: ${e.message ?: "không xác định"}")
        }
    }

    // ============================================================
    // HTTP — SUGGESTIONS (JSON)
    // ============================================================
    private suspend fun tryModelJson(
        model: String,
        messages: List<ChatMessage>
    ): AiSuggestResult {
        Log.d(TAG, "Using model=$model")
        return try {
            val response = api.chatCompletion(
                ChatRequest(
                    model = model,
                    messages = messages,
                    maxTokens = MAX_TOKENS,
                    temperature = TEMPERATURE
                )
            )
            if (response.error != null) {
                return AiSuggestResult.Failure(
                    response.error.message ?: "Yêu cầu AI bị từ chối"
                )
            }
            val content = response.choices.firstOrNull()?.message?.content?.trim()
            if (content.isNullOrBlank()) {
                return AiSuggestResult.Failure("AI không trả lời — vui lòng thử lại")
            }
            // Free-tier models occasionally wrap JSON in ```json fences despite our
            // explicit ask. Strip them defensively before parsing.
            val cleaned = content
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            parseSuggestions(cleaned)
        } catch (e: java.net.SocketTimeoutException) {
            AiSuggestResult.Failure("Mạng chậm — AI hết thời gian chờ")
        } catch (e: HttpException) {
            AiSuggestResult.Failure(extractHttpErrorMessage(e))
        } catch (e: java.io.IOException) {
            AiSuggestResult.Failure("Không thể kết nối đến AI — kiểm tra mạng")
        } catch (e: Exception) {
            Log.e(TAG, "Suggest request threw", e)
            AiSuggestResult.Failure("Lỗi AI: ${e.message ?: "không xác định"}")
        }
    }

    private fun parseSuggestions(raw: String): AiSuggestResult {
        return try {
            val parsed = jsonParser.decodeFromString(SuggestionsEnvelope.serializer(), raw)
            val suggestions = parsed.suggestions
                .filter { it.title.isNotBlank() }
                .map {
                    SuggestedHabit(
                        title = it.title.trim(),
                        emoji = it.emoji.ifBlank { "✨" }.trim(),
                        description = it.description.trim(),
                        difficulty = it.difficulty.uppercase().let { d ->
                            if (d in setOf("EASY", "MEDIUM", "HARD")) d else "MEDIUM"
                        },
                        estimatedImpact = it.estimatedImpact.trim(),
                        streakBenefit = it.streakBenefit.trim()
                    )
                }
            if (suggestions.isEmpty()) {
                AiSuggestResult.Failure("AI không tạo được gợi ý — thử lại sau")
            } else {
                AiSuggestResult.Success(suggestions)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse AI suggestions JSON: $raw", e)
            AiSuggestResult.Failure("AI trả về dữ liệu không đúng định dạng — thử lại")
        }
    }

    // ============================================================
    // ERROR MAPPING
    // ============================================================
    /**
     * Pulls the OpenRouter error JSON out of an [HttpException] and returns a
     * Vietnamese reason mapped per status code.
     *
     * OpenRouter error body shape (per docs):
     * `{ "error": { "message": "No auth credentials found", "code": 401 } }`
     */
    private fun extractHttpErrorMessage(e: HttpException): String {
        val code = e.code()
        val rawBody = try {
            e.response()?.errorBody()?.string().orEmpty()
        } catch (_: Throwable) {
            ""
        }
        Log.w(TAG, "OpenRouter HTTP $code body=$rawBody")

        val parsed = runCatching {
            jsonParser.decodeFromString(ErrorEnvelope.serializer(), rawBody).error?.message
        }.getOrNull()

        val detail = parsed?.takeIf { it.isNotBlank() } ?: rawBody.take(160)

        return when (code) {
            401 -> "Khóa AI không hợp lệ (401). Kiểm tra OPENROUTER_API_KEY trong local.properties và build lại."
            402 -> "Tài khoản OpenRouter cần nạp credit (402)${if (detail.isNotBlank()) ": $detail" else ""}"
            403 -> "OpenRouter từ chối yêu cầu (403)${if (detail.isNotBlank()) ": $detail" else ""}"
            404 -> "Model không tồn tại hoặc không truy cập được (404)${if (detail.isNotBlank()) ": $detail" else ""}"
            // Friendly 429: phrased as a passing moment rather than a hard fail.
            // The chain walker will keep trying the next model anyway, so this
            // string mostly surfaces in Logcat unless every model 429s.
            429 -> "AI đang hơi quá tải ✨ Đang thử model khác... (429)"
            in 500..599 -> "OpenRouter đang gặp sự cố ($code) — thử lại sau"
            else -> "Lỗi AI ($code)${if (detail.isNotBlank()) ": $detail" else ""}"
        }
    }

    // ============================================================
    // CANNED FALLBACK (last-resort, never empty UI)
    // ============================================================
    /**
     * Handwritten coaching message used when every model in [FALLBACK_MODELS]
     * fails. Honest about not knowing the user's specific numbers — names the
     * category so it doesn't feel like a generic error.
     */
    private fun cannedReview(categoryName: String): String = """
        Bạn đang duy trì nhóm "**$categoryName**" khá tốt 🌱. Hãy thử tăng độ ổn định bằng các thói quen nhỏ mỗi ngày — sự nhất quán quan trọng hơn sự hoàn hảo.

        - Chọn 1 thói quen quan trọng nhất hôm nay và làm nó trước, dù chỉ 5 phút.
        - Cho phép mình "lỡ một ngày" mà không bỏ luôn cả tuần.
    """.trimIndent()

    /**
     * Generic-but-useful suggestions that fit any category. Curated so even when
     * OpenRouter is fully unreachable the user gets four real, actionable ideas
     * rendered through the standard premium card.
     */
    private fun cannedSuggestions(): List<SuggestedHabit> = listOf(
        SuggestedHabit(
            title = "Khởi động 5 phút buổi sáng",
            emoji = "🌅",
            description = "Vài động tác nhẹ ngay sau khi thức dậy.",
            difficulty = "EASY",
            estimatedImpact = "Tạo đà tích cực cho cả ngày",
            streakBenefit = "Lặp lại 21 ngày sẽ thành phản xạ"
        ),
        SuggestedHabit(
            title = "Ghi 3 điều biết ơn trước khi ngủ",
            emoji = "📝",
            description = "Viết ngắn 3 điều bạn biết ơn hôm nay.",
            difficulty = "EASY",
            estimatedImpact = "Cải thiện tâm trạng và giấc ngủ",
            streakBenefit = "Càng đều đặn, tâm trí càng nhẹ nhõm"
        ),
        SuggestedHabit(
            title = "Học/đọc 15 phút mỗi ngày",
            emoji = "📚",
            description = "Một chủ đề bạn quan tâm, kể cả 1 trang sách.",
            difficulty = "MEDIUM",
            estimatedImpact = "Bồi đắp kiến thức theo thời gian",
            streakBenefit = "30 ngày = hơn 7 giờ học sâu"
        ),
        SuggestedHabit(
            title = "Đi bộ 10 phút sau bữa chính",
            emoji = "🚶",
            description = "Vận động nhẹ giúp tiêu hoá và tỉnh táo.",
            difficulty = "MEDIUM",
            estimatedImpact = "Tốt cho thể chất lẫn tinh thần",
            streakBenefit = "Đều đặn sẽ thay đổi mức năng lượng"
        )
    )

    // ============================================================
    // DTO / JSON
    // ============================================================
    @Serializable
    private data class SuggestionsEnvelope(val suggestions: List<SuggestionDto> = emptyList())

    @Serializable
    private data class SuggestionDto(
        val title: String = "",
        val emoji: String = "",
        val description: String = "",
        val difficulty: String = "MEDIUM",
        val estimatedImpact: String = "",
        val streakBenefit: String = ""
    )

    @Serializable
    private data class ErrorEnvelope(val error: ErrorBody? = null)

    @Serializable
    private data class ErrorBody(
        val message: String? = null,
        val code: Int? = null
    )

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private companion object {
        const val TAG = "AiHabitInsight"

        /**
         * Tried in order until one succeeds. New free models are added at the end
         * — the chain head is the model we expect to handle the steady-state load.
         */
        val FALLBACK_MODELS = listOf(
            "google/gemini-2.5-flash-preview:free",
            "google/gemini-2.0-flash-exp:free",
            "meta-llama/llama-3.3-70b-instruct:free",
            "mistralai/mistral-small-3.1-24b-instruct:free"
        )

        const val MAX_TOKENS = 120
        const val TEMPERATURE = 0.6

        const val RETRY_DELAY_MS = 800L
    }
}

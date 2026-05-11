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
 * Model strategy
 * - Primary: Gemini 2.0 Flash (free tier). Fastest free responses (1-3s typical),
 *   Vietnamese-fluent, JSON-mode reliable.
 * - Fallback: Llama 3.3 70B (free tier). Stronger reasoning, used if Gemini 429/5xx
 *   or empty-bodies. Same chat schema, prompt is identical.
 *
 * Retry & failure policy
 * - One retry on 429/5xx with 800ms backoff. Avoids hammering a free model during
 *   short rate-limit windows without queuing forever.
 * - 401/402/403/404 fail fast — retry won't help.
 * - HttpException → readable Vietnamese reason via [extractHttpErrorMessage].
 * - Final cross-model fallback: if the whole primary path (+retry) fails, the call
 *   is retried once on the fallback model. Two physical attempts max per AI flow.
 *
 * Output shaping
 * - max_tokens = 350 keeps both cost and latency low while still producing 3-4
 *   coherent paragraphs. Free-tier quotas are token-based, so capping pays back
 *   directly in calls-per-day.
 * - temperature = 0.6 (review) / 0.7 (suggestions) — review is more deterministic
 *   so the same stats produce a stable read, suggestions slightly hotter for
 *   variety on regenerate.
 */
class AiHabitInsightRepositoryImpl(
    private val api: OpenRouterApi
) : AiHabitInsightRepository {

    override suspend fun reviewHabitGroup(
        categoryName: String,
        stats: String,
        personality: AiCoachPersonality
    ): AiResult {
        val systemPrompt = buildString {
            append(personality.systemPromptPrefix)
            append("\n\n")
            append(
                """
                BẮT BUỘC: Trả lời bằng tiếng Việt. Chỉ dùng các con số có trong dữ liệu
                bên dưới — TUYỆT ĐỐI không bịa số liệu. Cấu trúc:
                1) Nhận xét tổng quát (1 câu súc tích).
                2) Điểm mạnh thấy được từ dữ liệu (1-2 câu, trích dẫn con số cụ thể).
                3) Một đề xuất cải thiện cụ thể (1-2 câu, hành động rõ ràng).
                Tổng cộng không quá 5 câu, không dùng tiêu đề số, viết liền mạch tự nhiên.
                Có thể dùng **bold** để nhấn điểm quan trọng và dấu "- " cho gạch đầu dòng
                nếu liệt kê.
                """.trimIndent()
            )
        }
        val userPrompt = """
            Hãy nhận xét cho người dùng về nhóm thói quen "$categoryName".
            Đây là dữ liệu thực tế của họ:
            $stats
        """.trimIndent()

        val messages = listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = userPrompt)
        )

        // Primary attempt (Gemini Flash). retryOnTransient handles 429/5xx with one
        // backed-off retry so a transient hiccup doesn't burn the cross-model fallback.
        val primary = retryOnTransient {
            tryModel(PRIMARY_MODEL, messages, maxTokens = REVIEW_MAX_TOKENS, temperature = 0.6)
        }
        if (primary is AiResult.Success) return primary

        Log.w(
            TAG,
            "Review primary ($PRIMARY_MODEL) failed → falling back to $FALLBACK_MODEL. " +
                "Reason: ${(primary as AiResult.Failure).message}"
        )
        return tryModel(FALLBACK_MODEL, messages, maxTokens = REVIEW_MAX_TOKENS, temperature = 0.6)
    }

    override suspend fun suggestHabits(
        categoryName: String,
        existingHabitTitles: List<String>,
        personality: AiCoachPersonality
    ): AiSuggestResult {
        val existingList = if (existingHabitTitles.isEmpty()) "không có"
        else existingHabitTitles.joinToString("; ")

        val systemPrompt = buildString {
            append(personality.systemPromptPrefix)
            append("\n\n")
            append(
                """
                BẮT BUỘC: Chỉ trả lời bằng JSON hợp lệ — KHÔNG kèm chữ giải thích bên
                ngoài, KHÔNG dùng ```json block. Cấu trúc:
                {
                  "suggestions": [
                    {
                      "title": "Tên thói quen ngắn gọn (tiếng Việt)",
                      "emoji": "1 emoji duy nhất",
                      "description": "1-2 câu ngắn, vì sao nên làm",
                      "difficulty": "EASY" | "MEDIUM" | "HARD",
                      "estimatedImpact": "Tác động dự kiến (1 câu, tiếng Việt)",
                      "streakBenefit": "Vì sao duy trì đều đặn lại đáng giá (1 câu)"
                    }
                  ]
                }
                Trả về đúng 4 gợi ý, đa dạng độ khó (ít nhất 1 EASY), không trùng với
                thói quen hiện có của người dùng. Toàn bộ tiếng Việt, ngắn gọn.
                """.trimIndent()
            )
        }
        val userPrompt = """
            Người dùng đang xem nhóm thói quen "$categoryName".
            Thói quen họ đã có trong nhóm này: $existingList
            Hãy gợi ý 4 thói quen mới phù hợp với chủ đề của nhóm.
        """.trimIndent()

        val messages = listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = userPrompt)
        )

        val primary = retryOnTransient {
            tryModelJson(PRIMARY_MODEL, messages)
        }
        if (primary is AiSuggestResult.Success) return primary

        Log.w(
            TAG,
            "Suggest primary ($PRIMARY_MODEL) failed → falling back to $FALLBACK_MODEL. " +
                "Reason: ${(primary as AiSuggestResult.Failure).message}"
        )
        return tryModelJson(FALLBACK_MODEL, messages)
    }

    /**
     * Wraps a single physical request and retries it once on the *same* model if the
     * failure looks transient (429 / 5xx / read timeout). Anything fatal (401, 402,
     * 403, 404, parse errors, IO) short-circuits — retrying won't help.
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

    private suspend fun tryModelJson(
        model: String,
        messages: List<ChatMessage>
    ): AiSuggestResult {
        Log.d(TAG, "Suggest call → model=$model maxTokens=$SUGGEST_MAX_TOKENS")
        return try {
            val response = api.chatCompletion(
                ChatRequest(
                    model = model,
                    messages = messages,
                    maxTokens = SUGGEST_MAX_TOKENS,
                    temperature = 0.7
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

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private suspend fun tryModel(
        model: String,
        messages: List<ChatMessage>,
        maxTokens: Int,
        temperature: Double
    ): AiResult {
        Log.d(TAG, "Review call → model=$model maxTokens=$maxTokens")
        return try {
            val response = api.chatCompletion(
                ChatRequest(
                    model = model,
                    messages = messages,
                    maxTokens = maxTokens,
                    temperature = temperature
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

    /**
     * Pulls the OpenRouter error JSON out of an [HttpException] and returns the
     * underlying reason. Without this, a 401 surfaces as the unhelpful string
     * "HTTP 401 " (= [HttpException.message]) and the user has no way to tell an
     * auth problem apart from a network problem.
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
            429 -> "Đã vượt giới hạn miễn phí — thử lại sau (429)"
            in 500..599 -> "OpenRouter đang gặp sự cố ($code) — thử lại sau"
            else -> "Lỗi AI ($code)${if (detail.isNotBlank()) ": $detail" else ""}"
        }
    }

    @Serializable
    private data class ErrorEnvelope(val error: ErrorBody? = null)

    @Serializable
    private data class ErrorBody(
        val message: String? = null,
        val code: Int? = null
    )

    private companion object {
        const val TAG = "AiHabitInsight"
        // Free-tier model order. Gemini Flash leads because it's the fastest free
        // chat model on OpenRouter at time of writing (1-3s typical) and handles
        // strict-JSON responses cleanly. Llama 70B is the fallback when Gemini
        // throttles or returns an empty body.
        const val PRIMARY_MODEL = "google/gemini-2.0-flash-exp:free"
        const val FALLBACK_MODEL = "meta-llama/llama-3.3-70b-instruct:free"

        // max_tokens budget. Tuned to fit a 5-sentence coaching reply or 4 short
        // habit suggestions. Bumping these increases free-tier quota burn linearly.
        const val REVIEW_MAX_TOKENS = 350
        const val SUGGEST_MAX_TOKENS = 350

        const val RETRY_DELAY_MS = 800L
    }
}

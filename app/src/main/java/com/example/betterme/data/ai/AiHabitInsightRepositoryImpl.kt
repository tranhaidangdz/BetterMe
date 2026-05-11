package com.example.betterme.data.ai

import android.util.Log
import com.example.betterme.data.ai.dto.ChatMessage
import com.example.betterme.data.ai.dto.ChatRequest
import com.example.betterme.domain.ai.AiCoachPersonality
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.ai.AiHabitInsightRepository.AiResult
import com.example.betterme.domain.ai.AiHabitInsightRepository.AiSuggestResult
import com.example.betterme.domain.ai.SuggestedHabit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * OpenRouter-backed implementation.
 *
 * Model strategy
 * - Primary: a free-tier Llama 3.3 70B model. Strong reasoning, generous free
 *   tier, Vietnamese-fluent.
 * - Fallback: a free-tier Gemini Flash model. If the primary 429s or 503s, the
 *   second call retries with the fallback model. Both share the same chat schema
 *   so the prompt is identical.
 *
 * Output shaping
 * - `max_tokens = 400` caps cost and forces concise replies (~3 short paragraphs).
 * - `temperature = 0.6` keeps responses on-brand without robotic-feeling repetition.
 * - System prompt is per-personality + a hard rule that the model must respond in
 *   Vietnamese — the app's primary language.
 *
 * Error normalization
 * - Network exceptions → AiResult.Failure with a user-friendly message.
 * - Empty `choices[0].message.content` → AiResult.Failure (some free models 200
 *   with an empty body when overloaded).
 * - HTTP errors caught at the OkHttp / Retrofit boundary; we don't surface raw
 *   stack traces to the UI.
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
                BẮT BUỘC: Trả lời bằng tiếng Việt. Đưa ra:
                1) Một câu nhận xét tổng thể (1-2 câu).
                2) Điểm mạnh thấy được từ dữ liệu (1-2 câu).
                3) Một đề xuất cải thiện cụ thể (1-2 câu).
                Tổng cộng không quá 5 câu, không dùng tiêu đề số, viết liền mạch tự nhiên.
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

        // First attempt with the primary model.
        val primary = tryModel(PRIMARY_MODEL, messages)
        if (primary is AiResult.Success) return primary

        Log.w(TAG, "Primary model failed — falling back. Reason: ${(primary as AiResult.Failure).message}")
        // Second attempt with the fallback. If this also fails, surface the
        // fallback's error since it's the more recent signal.
        return tryModel(FALLBACK_MODEL, messages)
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
                      "estimatedImpact": "Tác động dự kiến (1 câu, tiếng Việt)"
                    }
                  ]
                }
                Trả về đúng 4 gợi ý, đa dạng độ khó (ít nhất 1 EASY), không trùng với
                thói quen hiện có của người dùng. Toàn bộ tiếng Việt.
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

        val primary = tryModelJson(PRIMARY_MODEL, messages)
        if (primary is AiSuggestResult.Success) return primary

        Log.w(TAG, "Suggest primary failed — falling back: ${(primary as AiSuggestResult.Failure).message}")
        return tryModelJson(FALLBACK_MODEL, messages)
    }

    private suspend fun tryModelJson(
        model: String,
        messages: List<ChatMessage>
    ): AiSuggestResult {
        return try {
            val response = api.chatCompletion(
                ChatRequest(
                    model = model,
                    messages = messages,
                    maxTokens = 600,
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
            // Free-tier models occasionally wrap JSON in ```json fences despite our
            // explicit ask. Strip them defensively before parsing.
            val cleaned = content
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            parseSuggestions(cleaned)
        } catch (e: java.net.SocketTimeoutException) {
            AiSuggestResult.Failure("Mạng chậm — AI hết thời gian chờ")
        } catch (e: HttpException) {
            // Retrofit throws this on any non-2xx response. The actual reason
            // (invalid key, rate limit, payment required, model not found) is in
            // the JSON error body — surface it instead of "HTTP 401".
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
                        estimatedImpact = it.estimatedImpact.trim()
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
        val estimatedImpact: String = ""
    )

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private suspend fun tryModel(model: String, messages: List<ChatMessage>): AiResult {
        return try {
            val response = api.chatCompletion(
                ChatRequest(model = model, messages = messages)
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

        // Try to parse `{ "error": { "message": "..." } }`; fall back to raw body.
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
        // Both models are free tier on OpenRouter at time of writing. Keys
        // documented here so swapping is a one-line change.
        const val PRIMARY_MODEL = "meta-llama/llama-3.3-70b-instruct:free"
        const val FALLBACK_MODEL = "google/gemini-2.0-flash-exp:free"
    }
}

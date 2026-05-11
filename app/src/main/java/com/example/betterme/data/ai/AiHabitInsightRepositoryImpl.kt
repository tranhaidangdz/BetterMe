package com.example.betterme.data.ai

import android.util.Log
import com.example.betterme.data.ai.dto.ChatMessage
import com.example.betterme.data.ai.dto.ChatRequest
import com.example.betterme.domain.ai.AiCoachPersonality
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.ai.AiHabitInsightRepository.AiResult

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
        } catch (e: java.io.IOException) {
            AiResult.Failure("Không thể kết nối đến AI — kiểm tra mạng")
        } catch (e: Exception) {
            Log.e(TAG, "AI request threw", e)
            AiResult.Failure("Lỗi AI: ${e.message ?: "không xác định"}")
        }
    }

    private companion object {
        const val TAG = "AiHabitInsight"
        // Both models are free tier on OpenRouter at time of writing. Keys
        // documented here so swapping is a one-line change.
        const val PRIMARY_MODEL = "meta-llama/llama-3.3-70b-instruct:free"
        const val FALLBACK_MODEL = "google/gemini-2.0-flash-exp:free"
    }
}

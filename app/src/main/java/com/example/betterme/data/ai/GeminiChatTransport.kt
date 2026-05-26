package com.example.betterme.data.ai

import android.util.Log
import com.example.betterme.BuildConfig
import com.example.betterme.data.ai.dto.ChatChoice
import com.example.betterme.data.ai.dto.ChatError
import com.example.betterme.data.ai.dto.ChatMessage
import com.example.betterme.data.ai.dto.ChatResponse
import com.example.betterme.data.ai.dto.GeminiContent
import com.example.betterme.data.ai.dto.GeminiGenerateRequest
import com.example.betterme.data.ai.dto.GeminiGenerationConfig
import com.example.betterme.data.ai.dto.GeminiPart
import com.example.betterme.data.ai.dto.GeminiThinkingConfig
import retrofit2.HttpException

/**
 * [ChatTransport] backed by Google's native Gemini REST endpoint. Translates the
 * OpenAI-style ChatMessage list into Gemini's `contents` + `systemInstruction`
 * schema on the way in, and the Gemini candidate response into the canonical
 * [ChatResponse] shape on the way out, so the rest of the AI module never has
 * to know which provider answered.
 *
 * Mapping notes
 *  - The "system" message becomes [GeminiGenerateRequest.systemInstruction];
 *    only the first one is honored (matches OpenAI behavior).
 *  - "user" / "assistant" messages become [GeminiContent] with role "user" /
 *    "model" respectively. Other roles are coerced to "user".
 *  - Generation config carries temperature + maxOutputTokens. Tools, JSON
 *    response-mime, safety overrides etc. are intentionally not exposed yet —
 *    add only when a chain in the repo actually needs them.
 */
class GeminiChatTransport(
    private val api: GeminiApi
) : ChatTransport {

    override suspend fun chat(
        model: String,
        apiKey: String,
        messages: List<ChatMessage>,
        maxTokens: Int,
        temperature: Double,
        responseMimeType: String?
    ): ChatResponse {
        val systemText = messages.firstOrNull { it.role == ROLE_SYSTEM }?.content
        val conversation = messages.filter { it.role != ROLE_SYSTEM }

        val contents = conversation.map { msg ->
            GeminiContent(
                role = when (msg.role) {
                    ROLE_ASSISTANT -> ROLE_MODEL
                    else -> ROLE_USER
                },
                parts = listOf(GeminiPart(text = msg.content))
            )
        }

        val systemInstruction = systemText?.let {
            GeminiContent(role = null, parts = listOf(GeminiPart(it)))
        }

        val request = GeminiGenerateRequest(
            contents = contents,
            systemInstruction = systemInstruction,
            generationConfig = GeminiGenerationConfig(
                temperature = temperature,
                maxOutputTokens = maxTokens,
                // When the caller asks for JSON, pin the response MIME type so
                // Gemini refuses to wrap the JSON in ```json fences or chatty
                // commentary — both of which break our serializer. Plain-text
                // surfaces (review card) pass null and let the model emit
                // markdown.
                responseMimeType = responseMimeType,
                // Disable Gemini 2.5's internal "thinking" pass. Default
                // behaviour burns ~70 thinking tokens before any visible
                // output — with our 120-500 max-output budgets that
                // truncates the answer and surfaces `finishReason =
                // MAX_TOKENS` with a near-empty body. BetterMe prompts are
                // schema-shaped (strict JSON) or markdown-section-shaped, so
                // the deeper reasoning step gives no measurable quality win.
                thinkingConfig = GeminiThinkingConfig(thinkingBudget = 0)
            )
        )

        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "POST /v1beta/models/$model:generateContent " +
                    "contents=${contents.size} sysInstr=${systemInstruction != null} " +
                    "maxTokens=$maxTokens temp=$temperature " +
                    "mime=${responseMimeType ?: "(text)"} " +
                    "key=${ApiKeyPool.mask(apiKey)}"
            )
        }

        val response = try {
            api.generateContent(
                model = model,
                apiKey = apiKey,
                body = request
            )
        } catch (e: HttpException) {
            // Read the body ONCE here so we always see Google's error envelope in
            // Logcat — `extractHttpErrorMessage` reads it again on the repo side
            // for user-facing copy, but at this layer we want the raw payload so
            // bad-request shape issues are immediately obvious during demo / triage.
            val body = try { e.response()?.errorBody()?.string().orEmpty() } catch (_: Throwable) { "" }
            Log.w(TAG, "Gemini HTTP ${e.code()} model=$model body=$body")
            throw e
        } catch (e: Throwable) {
            Log.w(TAG, "Gemini transport threw ${e.javaClass.simpleName}: ${e.message}", e)
            throw e
        }

        // Normalize → ChatResponse. Concatenate every part on the first
        // candidate; Gemini occasionally splits a single reply across multiple
        // parts (e.g., text + executable code) and a naive `.first()` would drop
        // the rest.
        val firstCandidate = response.candidates.firstOrNull()
        val text = firstCandidate?.content?.parts
            ?.joinToString(separator = "") { it.text }
            .orEmpty()

        val choices = if (text.isNotEmpty()) {
            listOf(
                ChatChoice(
                    index = 0,
                    message = ChatMessage(role = ROLE_ASSISTANT, content = text),
                    finishReason = firstCandidate?.finishReason
                )
            )
        } else emptyList()

        val error = response.error?.let {
            ChatError(message = it.message, code = it.code)
        }

        return ChatResponse(
            id = null,
            model = model,
            choices = choices,
            error = error
        )
    }

    private companion object {
        const val TAG = "GeminiTransport"
        const val ROLE_SYSTEM = "system"
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val ROLE_MODEL = "model"
    }
}

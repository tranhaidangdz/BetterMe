package com.example.betterme.data.ai.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Minimal wire types for Google's `generativelanguage.googleapis.com` REST API
 * — endpoint `models/{model}:generateContent`. We only model the subset
 * BetterMe actually reads: contents + generationConfig in, candidates[0].text out,
 * plus a top-level error envelope for non-2xx replies that still parse as JSON.
 *
 * Two structural differences from OpenAI / Gemini that matter here:
 * 1. There is no `messages: [{role, content}]` array. Chat history goes in
 *    `contents` as a list of {role, parts:[{text}]}; the "system" message goes
 *    in a separate top-level `systemInstruction` field.
 * 2. The model echoes "model" as the role on the reply (not "assistant").
 *
 * The router/transport normalizes these into the existing [ChatResponse] shape
 * so the rest of the AI module stays unchanged.
 */
@Serializable
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    @SerialName("systemInstruction") val systemInstruction: GeminiContent? = null,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiContent(
    /** "user" or "model"; absent on systemInstruction. */
    val role: String? = null,
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Double? = null,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int? = null,
    @SerialName("responseMimeType") val responseMimeType: String? = null,
    /**
     * Disables Gemini 2.5's internal "thinking" pass when set to a
     * [GeminiThinkingConfig] with `thinkingBudget = 0`. Critical for our
     * coach use cases: with default thinking enabled, the model burns ~70
     * tokens of internal reasoning before producing visible output, and
     * with our typical maxOutputTokens budgets (120-500) the user sees
     * truncated / empty responses with `finishReason = MAX_TOKENS`.
     *
     * Our prompts already supply a tightly-structured JSON schema or
     * markdown layout, so the deeper reasoning pass gives no quality win
     * for the latency / token cost.
     */
    @SerialName("thinkingConfig") val thinkingConfig: GeminiThinkingConfig? = null
)

@Serializable
data class GeminiThinkingConfig(
    /**
     * 0 = disable thinking entirely. Positive values cap the thinking-token
     * budget. We pass 0 from [com.example.betterme.data.ai.GeminiChatTransport]
     * across every chain because every BetterMe AI surface is JSON-schema or
     * markdown-shape driven — Gemini 2.5 thinking adds no quality but eats
     * the visible output budget.
     */
    @SerialName("thinkingBudget") val thinkingBudget: Int? = null
)

@Serializable
data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val error: GeminiError? = null
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
    @SerialName("finishReason") val finishReason: String? = null,
    val index: Int = 0
)

@Serializable
data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

package com.example.betterme.data.ai.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire types for OpenRouter's `POST /api/v1/chat/completions` endpoint. Models the
 * subset of the OpenAI-compatible schema we actually use — no kitchen-sink fields.
 *
 * Keep these data classes minimal: every extra `@Serializable` field is one more
 * deserialization that runs on the hot path. The full OpenRouter schema is large;
 * we deliberately ignore everything we don't read (the OkHttp + retrofit-kotlinx
 * converter is configured with `ignoreUnknownKeys = true` so unknown fields are
 * silently dropped).
 */
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    /** Hard cap on response length — keeps cost predictable and replies concise. */
    @SerialName("max_tokens") val maxTokens: Int = 400,
    /** Lower temperature = more deterministic, on-brand coaching tone. */
    val temperature: Double = 0.6
)

@Serializable
data class ChatMessage(
    /** "system" | "user" | "assistant" */
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<ChatChoice> = emptyList(),
    /** Present on free-tier rate-limit responses; surfaced in repository errors. */
    val error: ChatError? = null
)

@Serializable
data class ChatChoice(
    val index: Int = 0,
    val message: ChatMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class ChatError(
    val message: String? = null,
    val code: Int? = null
)

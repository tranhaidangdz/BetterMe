package com.example.betterme.data.ai.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Provider-neutral chat completion wire types. Every [com.example.betterme.data.ai.ChatTransport]
 * implementation hands the router a [ChatResponse] regardless of which upstream
 * answered, so the repository never has to branch on provider.
 *
 * Kept minimal on purpose — every extra `@Serializable` field is one more JSON
 * decode on the hot path. The Gemini transport translates Gemini's native schema
 * into this shape; if a second provider is ever added, it does the same.
 */

@Serializable
data class ChatMessage(
    /** "system" | "user" | "assistant" — the OpenAI-style role taxonomy. */
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<ChatChoice> = emptyList(),
    /** Body-level error envelope. Present on quota / rate-limit responses
     *  that still arrived as HTTP 200 with an error payload. */
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

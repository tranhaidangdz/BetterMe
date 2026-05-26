package com.example.betterme.data.ai

import com.example.betterme.data.ai.dto.ChatMessage
import com.example.betterme.data.ai.dto.ChatResponse

/**
 * Provider-agnostic chat call. Each provider (Gemini today, future Claude /
 * Together / Groq / Fireworks / DeepInfra) implements this once and returns a normalized
 * [ChatResponse] so the repository's chain walker and parser never branch on
 * provider type.
 *
 * Contract:
 * - On success → returns a populated [ChatResponse]. May still contain a body-
 *   level `error` envelope on free-tier rate-limits; callers (and the router)
 *   leave that for the repository's per-attempt failure mapping.
 * - On HTTP errors → throws [retrofit2.HttpException] so the router can map the
 *   status code to a category and decide whether to cool the key + rotate.
 * - On network/timeout → throws the underlying [java.io.IOException] /
 *   [java.net.SocketTimeoutException] / [java.net.UnknownHostException] so the
 *   chain walker can categorize as TIMEOUT / NO_NETWORK and advance to the
 *   next model without burning a key cooldown.
 *
 * Implementations are stateless; the API key arrives per-call so a single
 * Retrofit instance can serve every key in the pool.
 */
interface ChatTransport {

    suspend fun chat(
        model: String,
        apiKey: String,
        messages: List<ChatMessage>,
        maxTokens: Int,
        temperature: Double
    ): ChatResponse
}

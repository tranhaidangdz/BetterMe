package com.example.betterme.data.ai

import com.example.betterme.data.ai.dto.ChatMessage
import com.example.betterme.data.ai.dto.ChatRequest
import com.example.betterme.data.ai.dto.ChatResponse

/**
 * [ChatTransport] backed by [OpenRouterApi]. Builds the canonical OpenAI-style
 * [ChatRequest] and lets Retrofit handle the rest. Auth is passed as a per-call
 * Bearer token so the same Retrofit instance serves every key in the pool.
 *
 * Returns the OpenRouter [ChatResponse] verbatim — it's already the normalized
 * shape the rest of the AI module reads.
 */
class OpenRouterChatTransport(
    private val api: OpenRouterApi
) : ChatTransport {

    override suspend fun chat(
        model: String,
        apiKey: String,
        messages: List<ChatMessage>,
        maxTokens: Int,
        temperature: Double
    ): ChatResponse = api.chatCompletion(
        authorization = "Bearer $apiKey",
        body = ChatRequest(
            model = model,
            messages = messages,
            maxTokens = maxTokens,
            temperature = temperature
        )
    )
}

package com.example.betterme.data.ai

import com.example.betterme.data.ai.dto.ChatRequest
import com.example.betterme.data.ai.dto.ChatResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Retrofit client for OpenRouter's OpenAI-compatible chat-completions endpoint.
 *
 * Base URL is fixed to `https://openrouter.ai/api/v1/` in [OpenRouterNetwork].
 * Per OpenRouter's analytics policy, every call should include `HTTP-Referer` and
 * `X-Title` headers so usage on the dashboard is attributable to BetterMe. These
 * headers are added here statically.
 *
 * The Bearer token is a **per-call** `@Header` parameter rather than a network-
 * level interceptor — the router rotates keys across requests, so the value can
 * differ from one call to the next on the same Retrofit instance.
 */
interface OpenRouterApi {

    @Headers(
        "HTTP-Referer: https://github.com/tranhaidangdz/BetterMe",
        "X-Title: BetterMe Habit Coach"
    )
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body body: ChatRequest
    ): ChatResponse
}

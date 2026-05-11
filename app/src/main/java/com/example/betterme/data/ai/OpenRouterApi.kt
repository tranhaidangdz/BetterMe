package com.example.betterme.data.ai

import com.example.betterme.data.ai.dto.ChatRequest
import com.example.betterme.data.ai.dto.ChatResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Retrofit client for OpenRouter's OpenAI-compatible chat-completions endpoint.
 *
 * Base URL is fixed to `https://openrouter.ai/api/v1/` in [OpenRouterNetwork].
 * Per OpenRouter's analytics policy, every call should include `HTTP-Referer` and
 * `X-Title` headers so usage on the dashboard is attributable to BetterMe. These
 * headers are added here statically — the per-request Bearer token is added by an
 * OkHttp interceptor configured in the network module.
 */
interface OpenRouterApi {

    @Headers(
        "HTTP-Referer: https://github.com/tranhaidangdz/BetterMe",
        "X-Title: BetterMe Habit Coach"
    )
    @POST("chat/completions")
    suspend fun chatCompletion(@Body body: ChatRequest): ChatResponse
}

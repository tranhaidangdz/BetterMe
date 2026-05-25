package com.example.betterme.data.ai

import com.example.betterme.data.ai.dto.GeminiGenerateRequest
import com.example.betterme.data.ai.dto.GeminiGenerateResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit client for Google's native Gemini REST endpoint.
 *
 * Base URL is fixed to `https://generativelanguage.googleapis.com/v1beta/` in
 * [GeminiNetwork]. The API key is passed as the `x-goog-api-key` header per
 * Google's recommendation (query-param form also works but headers don't show
 * up in URLs / proxy logs). The model slug is path-segmented and bound at the
 * call site by [GeminiChatTransport] so a single Retrofit instance can hit any
 * model the router asks for.
 */
interface GeminiApi {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body body: GeminiGenerateRequest
    ): GeminiGenerateResponse
}

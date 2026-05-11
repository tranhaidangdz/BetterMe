package com.example.betterme.data.ai

import com.example.betterme.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Constructs the singleton [OpenRouterApi] used by the AI module.
 *
 * - JSON converter is configured with `ignoreUnknownKeys = true` so the OpenRouter
 *   response can grow new fields without breaking us.
 * - OkHttp logging is **debug-build-only** — never prints prompts or completions in
 *   release builds.
 * - Read timeout bumped to 60s because some free-tier models (Llama 70B, Gemini
 *   Flash) take 20-30s on cold starts.
 *
 * The API key arrives at request time via a per-request `Authorization` interceptor
 * so a future settings screen can let users supply their own key without rebuilding
 * the Retrofit instance.
 */
object OpenRouterNetwork {

    private const val BASE_URL = "https://openrouter.ai/api/v1/"

    fun create(apiKeyProvider: () -> String): OpenRouterApi {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        val authInterceptor = Interceptor { chain ->
            val key = apiKeyProvider()
            val request = chain.request().newBuilder().apply {
                if (key.isNotBlank()) {
                    header("Authorization", "Bearer $key")
                }
            }.build()
            chain.proceed(request)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(OpenRouterApi::class.java)
    }
}

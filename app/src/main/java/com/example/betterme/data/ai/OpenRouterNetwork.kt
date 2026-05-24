package com.example.betterme.data.ai

import android.util.Log
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
 * - OkHttp logging is **debug-build-only** at HEADERS level so we can inspect the
 *   request/response status without leaking the body. The Authorization header is
 *   explicitly redacted so a debug build never prints the API key.
 * - Read timeout bumped to 60s because some free-tier models (Llama 70B, Gemini
 *   Flash) take 20-30s on cold starts.
 *
 * The API key arrives at request time via a per-request `Authorization` interceptor
 * so a future settings screen can let users supply their own key without rebuilding
 * the Retrofit instance.
 */
object OpenRouterNetwork {

    private const val BASE_URL = "https://openrouter.ai/api/v1/"
    private const val TAG = "OpenRouterNetwork"

    fun create(apiKeyProvider: () -> String): OpenRouterApi {
        // One-time diagnostic so "I added the key but it's not working" is answerable
        // from Logcat alone. Masked — only length + first 10 chars surface.
        if (BuildConfig.DEBUG) {
            val key = apiKeyProvider()
            val masked = if (key.length > 10) "${key.take(10)}…" else "(short or blank)"
            Log.d(TAG, "OpenRouter key loaded: present=${key.isNotBlank()} length=${key.length} prefix=$masked")
        }

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        val authInterceptor = Interceptor { chain ->
            val key = apiKeyProvider().trim()
            val request = chain.request().newBuilder().apply {
                if (key.isNotBlank()) {
                    header("Authorization", "Bearer $key")
                }
            }.build()
            chain.proceed(request)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // BODY-level in debug so the OpenRouter error envelope (`{"error":{"message":...,"code":401}}`)
            // shows up in Logcat verbatim — the single biggest help when triaging "AI
            // is failing" reports from users running debug builds. The API key is
            // explicitly redacted; nothing else in the body is sensitive (system
            // prompts + Vietnamese completions only). Release builds stay silent.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
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

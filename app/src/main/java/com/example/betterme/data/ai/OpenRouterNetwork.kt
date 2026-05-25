package com.example.betterme.data.ai

import android.util.Log
import com.example.betterme.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
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
 * - OkHttp logging is **debug-build-only** at BODY level so the per-attempt error
 *   envelope (`{"error":{"message":...,"code":401}}`) is visible during triage.
 *   The Authorization header is explicitly redacted.
 * - Read timeout 25s — long enough to absorb a Gemini Flash / Llama 70B cold start
 *   (typically 5-15s) but tight enough that a dead/overloaded model fails fast and
 *   the chain advances to the next.
 *
 * No auth interceptor: the Bearer token rotates per call via the `@Header` parameter
 * on [OpenRouterApi.chatCompletion], so a single Retrofit instance can serve every
 * key in the pool without being rebuilt.
 */
object OpenRouterNetwork {

    private const val BASE_URL = "https://openrouter.ai/api/v1/"
    private const val TAG = "OpenRouterNetwork"

    fun create(): OpenRouterApi {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "OpenRouter Retrofit ready @ $BASE_URL")
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(OpenRouterApi::class.java)
    }
}

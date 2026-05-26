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
 * Constructs the singleton [GeminiApi] used by the AI module's Gemini transport.
 *
 * Mirrors [GeminiNetwork] in style:
 * - `ignoreUnknownKeys = true` on the JSON converter so a Google response can
 *   grow new fields (safetyRatings, citationMetadata, etc.) without breaking us.
 * - OkHttp logging is debug-build-only at BODY level so the per-attempt error
 *   envelope (`{"error":{"code":429,"message":"…","status":"RESOURCE_EXHAUSTED"}}`)
 *   is visible during triage. The `x-goog-api-key` header is redacted so a
 *   debug build never prints the secret.
 * - Read timeout 25 s — matches the Gemini transport so the router's
 *   bounded retry budget is symmetric.
 *
 * No auth interceptor here: the API key arrives as a per-call header from the
 * key pool, so the same Retrofit instance can rotate between keys without
 * being rebuilt.
 */
object GeminiNetwork {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val TAG = "GeminiNetwork"

    fun create(): GeminiApi {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
            // The actual secret travels in `x-goog-api-key` per Google's
            // recommendation. Redact it in debug logs so screenshots of
            // Logcat don't leak the key.
            redactHeader("x-goog-api-key")
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Gemini Retrofit ready @ $BASE_URL")
        }

        return retrofit.create(GeminiApi::class.java)
    }
}

package com.example.betterme.data.share

import com.example.betterme.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Constructs the singleton [ShareApi]. Pattern mirrors
 * [com.example.betterme.data.ai.OpenRouterNetwork]:
 *
 *  - JSON converter with `ignoreUnknownKeys` so server schema can grow.
 *  - Per-request Authorization header (set inside the repository) so
 *    we don't bake the user's ID token into the OkHttpClient.
 *  - HEADERS-level logging in debug builds; redacted Authorization so
 *    Logcat never leaks the bearer.
 *
 * Base URL comes from [BuildConfig.SHARE_FUNCTIONS_BASE_URL], which
 * is set in `app/build.gradle.kts`. The default points at the
 * Firebase Functions emulator address so a debug build without the
 * functions deployed still has something resolvable.
 */
object ShareApiClient {

    fun create(): ShareApi {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.SHARE_FUNCTIONS_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(ShareApi::class.java)
    }
}

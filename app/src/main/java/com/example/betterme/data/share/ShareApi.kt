package com.example.betterme.data.share

import com.example.betterme.data.share.dto.CreateShareRequest
import com.example.betterme.data.share.dto.CreateShareResponse
import com.example.betterme.data.share.dto.GetShareResponse
import com.example.betterme.data.share.dto.VerifyShareResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface for the share Cloud Functions.
 *
 * Bearer token is passed per-request via the `Authorization` header
 * — same pattern OpenRouterNetwork uses for the API key. The Firebase
 * ID token is acquired by [com.example.betterme.data.share.ShareRepositoryImpl]
 * just before the call.
 */
interface ShareApi {

    @POST("createShare")
    suspend fun createShare(
        @Header("Authorization") bearer: String,
        @Body body: CreateShareRequest
    ): CreateShareResponse

    @GET("getShare")
    suspend fun getShare(
        @Query("id") shareId: String
    ): GetShareResponse

    @GET("verifyShare")
    suspend fun verifyShare(
        @Query("id") shareId: String
    ): VerifyShareResponse
}

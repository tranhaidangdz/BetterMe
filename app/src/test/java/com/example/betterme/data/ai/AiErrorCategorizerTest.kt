package com.example.betterme.data.ai

import com.example.betterme.domain.ai.AiErrorCategory
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Verifies that every triage path the AI repo can hit maps to the right user-facing
 * category. The pickWorst priority table is the load-bearing part — when multiple
 * models fail with different codes, the user sees the most-actionable bucket first.
 */
class AiErrorCategorizerTest {

    // ============================================================
    // categorize(Throwable)
    // ============================================================

    @Test
    fun `socket timeout maps to TIMEOUT`() {
        assertEquals(AiErrorCategory.TIMEOUT, AiErrorCategorizer.categorize(SocketTimeoutException()))
    }

    @Test
    fun `unknown host maps to NO_NETWORK`() {
        assertEquals(AiErrorCategory.NO_NETWORK, AiErrorCategorizer.categorize(UnknownHostException()))
    }

    @Test
    fun `generic IO error maps to NO_NETWORK`() {
        assertEquals(AiErrorCategory.NO_NETWORK, AiErrorCategorizer.categorize(IOException()))
    }

    @Test
    fun `serialization error maps to PARSE`() {
        assertEquals(AiErrorCategory.PARSE, AiErrorCategorizer.categorize(SerializationException("bad json")))
    }

    @Test
    fun `unrecognized throwable maps to UNKNOWN`() {
        assertEquals(AiErrorCategory.UNKNOWN, AiErrorCategorizer.categorize(IllegalStateException("anything")))
    }

    // ============================================================
    // categorizeHttp(code)
    // ============================================================

    @Test
    fun `http codes map correctly`() {
        assertEquals(AiErrorCategory.INVALID_KEY, AiErrorCategorizer.categorizeHttp(401))
        assertEquals(AiErrorCategory.INVALID_KEY, AiErrorCategorizer.categorizeHttp(403))
        assertEquals(AiErrorCategory.QUOTA_EXCEEDED, AiErrorCategorizer.categorizeHttp(402))
        assertEquals(AiErrorCategory.MODEL_UNAVAILABLE, AiErrorCategorizer.categorizeHttp(404))
        assertEquals(AiErrorCategory.MODEL_UNAVAILABLE, AiErrorCategorizer.categorizeHttp(400))
        assertEquals(AiErrorCategory.RATE_LIMITED, AiErrorCategorizer.categorizeHttp(429))
        assertEquals(AiErrorCategory.SERVER_ERROR, AiErrorCategorizer.categorizeHttp(500))
        assertEquals(AiErrorCategory.SERVER_ERROR, AiErrorCategorizer.categorizeHttp(502))
        assertEquals(AiErrorCategory.SERVER_ERROR, AiErrorCategorizer.categorizeHttp(503))
        assertEquals(AiErrorCategory.SERVER_ERROR, AiErrorCategorizer.categorizeHttp(504))
        assertEquals(AiErrorCategory.UNKNOWN, AiErrorCategorizer.categorizeHttp(418))
    }

    // ============================================================
    // pickWorst — priority must match the user's mental model:
    // "fix the auth/quota problem first; everything else can be retried"
    // ============================================================

    @Test
    fun `empty list defaults to UNKNOWN`() {
        assertEquals(AiErrorCategory.UNKNOWN, AiErrorCategorizer.pickWorst(emptyList()))
    }

    @Test
    fun `invalid key beats everything`() {
        val cats = listOf(
            AiErrorCategory.MODEL_UNAVAILABLE,
            AiErrorCategory.RATE_LIMITED,
            AiErrorCategory.INVALID_KEY,
            AiErrorCategory.SERVER_ERROR
        )
        assertEquals(AiErrorCategory.INVALID_KEY, AiErrorCategorizer.pickWorst(cats))
    }

    @Test
    fun `quota beats rate limit and server error`() {
        val cats = listOf(
            AiErrorCategory.SERVER_ERROR,
            AiErrorCategory.RATE_LIMITED,
            AiErrorCategory.QUOTA_EXCEEDED
        )
        assertEquals(AiErrorCategory.QUOTA_EXCEEDED, AiErrorCategorizer.pickWorst(cats))
    }

    @Test
    fun `no network beats timeout when both present`() {
        val cats = listOf(AiErrorCategory.TIMEOUT, AiErrorCategory.NO_NETWORK)
        assertEquals(AiErrorCategory.NO_NETWORK, AiErrorCategorizer.pickWorst(cats))
    }

    @Test
    fun `every model 404 surfaces MODEL_UNAVAILABLE`() {
        // Common cause: free-tier model list went stale. UI tells the user the
        // ":free" lineup is out of date.
        val cats = List(4) { AiErrorCategory.MODEL_UNAVAILABLE }
        assertEquals(AiErrorCategory.MODEL_UNAVAILABLE, AiErrorCategorizer.pickWorst(cats))
    }

    @Test
    fun `mixed 429 and 404 picks RATE_LIMITED over MODEL_UNAVAILABLE`() {
        val cats = listOf(
            AiErrorCategory.MODEL_UNAVAILABLE,
            AiErrorCategory.RATE_LIMITED,
            AiErrorCategory.MODEL_UNAVAILABLE
        )
        assertEquals(AiErrorCategory.RATE_LIMITED, AiErrorCategorizer.pickWorst(cats))
    }

    @Test
    fun `unknown is the floor`() {
        // If every attempt produced UNKNOWN (e.g. unexpected exception types),
        // pickWorst preserves UNKNOWN.
        val cats = listOf(AiErrorCategory.UNKNOWN, AiErrorCategory.UNKNOWN)
        assertEquals(AiErrorCategory.UNKNOWN, AiErrorCategorizer.pickWorst(cats))
    }
}

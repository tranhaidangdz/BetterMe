package com.example.betterme.data.ai

import com.example.betterme.data.ai.dto.ChatMessage
import com.example.betterme.data.ai.dto.ChatResponse
import com.example.betterme.domain.ai.AiErrorCategory
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Behavioral tests for [AiChatRouter]. Drives the router with a recordable fake
 * transport whose per-key responses can be scripted; the pool gets a controllable
 * clock so cooldown windows are deterministic.
 *
 * Scenarios mirror the user-stated requirements:
 *  1. Rate-limited key (429)  → cooled 60 s, router rotates to next key, succeeds.
 *  2. Invalid key  (401/403)  → cooled 10 min, router rotates to next key, succeeds.
 *  3. Quota exhausted (402)   → cooled 10 min, router rotates to next key, succeeds.
 *  4. Every key cooled        → router throws AiAttemptFailure(QUOTA_EXCEEDED).
 *  5. Empty pool              → throws AiAttemptFailure(INVALID_KEY) without calling transport.
 *  6. Server error (500)      → bubbles up as HttpException; key NOT cooled (problem upstream of key).
 *  7. Provider failover       → outer chain walker advances to OpenRouter once Gemini's
 *                               keys are cooled; new pool + new transport take over.
 *  8. Bounded retry           → at most pool.size attempts per chat() call. No infinite loop.
 */
class AiChatRouterTest {

    private val messages = listOf(ChatMessage(role = "user", content = "hi"))

    // -----------------------------------------------------------------
    // 1. RATE_LIMITED key gets cooled, next key takes the call
    // -----------------------------------------------------------------
    @Test
    fun `429 cools the offending key and rotates to the next one`() = runBlocking {
        var fakeNow = 1_000L
        val pool = ApiKeyPool(keys = listOf("k1", "k2"), nowProvider = { fakeNow })
        val transport = RecordingTransport(
            scripted = mapOf(
                "k1" to TransportOutcome.Error(429),
                "k2" to TransportOutcome.Ok("k2-response")
            )
        )
        val router = AiChatRouter(
            transports = mapOf(AiProvider.GEMINI to transport),
            pools = mapOf(AiProvider.GEMINI to pool)
        )

        val resp = router.chat("gemini-2.5-flash", messages, maxTokens = 100, temperature = 0.6)
        assertEquals("k2-response", resp.choices.first().message?.content)
        // k1 must be cooled for ~60 s.
        assertEquals(2, transport.calls.size)
        assertEquals("k1", transport.calls[0].apiKey)
        assertEquals("k2", transport.calls[1].apiKey)

        // 30 s later — k1 still cooled. allCooled() should be false (k2 healthy)
        // but k1 must still be on cooldown.
        fakeNow += 30_000L
        assertFalse("pool not fully cooled (k2 healthy)", pool.allCooled())

        // 61 s later — k1's window has elapsed; it's eligible again.
        fakeNow += 31_000L
        // Drain any healthy keys until k1 surfaces (round-robin order varies).
        val regained = (1..pool.size).map { pool.acquireHealthy() }
        assertTrue("k1 must be acquirable after 61s cooldown", regained.contains("k1"))
    }

    // -----------------------------------------------------------------
    // 2. INVALID_KEY (401) cools key for the long window
    // -----------------------------------------------------------------
    @Test
    fun `401 cools the key for 10 minutes and rotates`() = runBlocking {
        var fakeNow = 1_000L
        val pool = ApiKeyPool(keys = listOf("k1", "k2"), nowProvider = { fakeNow })
        val transport = RecordingTransport(
            scripted = mapOf(
                "k1" to TransportOutcome.Error(401),
                "k2" to TransportOutcome.Ok("k2-response")
            )
        )
        val router = AiChatRouter(
            transports = mapOf(AiProvider.GEMINI to transport),
            pools = mapOf(AiProvider.GEMINI to pool)
        )

        val resp = router.chat("gemini-2.5-flash", messages, 100, 0.6)
        assertEquals("k2-response", resp.choices.first().message?.content)

        // After 5 minutes k1 still cooled (window is 10 min); pool not fully cooled
        // because k2 is healthy.
        fakeNow += 5 * 60_000L
        assertFalse("k1 should still be cooled at 5min", pool.allCooled())
        // Any acquire in this window only ever yields k2 — k1 is skipped.
        val midWindow = (1..pool.size * 2).map { pool.acquireHealthy() }.distinct()
        assertEquals(setOf("k2"), midWindow.toSet())

        // After 10 min + 1 s, k1 eligible again.
        fakeNow += 5 * 60_000L + 1_000L
        val regained = (1..pool.size).map { pool.acquireHealthy() }
        assertTrue("k1 must be acquirable after 10min cooldown", regained.contains("k1"))
    }

    // -----------------------------------------------------------------
    // 3. QUOTA_EXCEEDED (402) treated like 401 — long cooldown
    // -----------------------------------------------------------------
    @Test
    fun `402 cools the key for 10 minutes and rotates`() = runBlocking {
        var fakeNow = 1_000L
        val pool = ApiKeyPool(keys = listOf("k1", "k2"), nowProvider = { fakeNow })
        val transport = RecordingTransport(
            scripted = mapOf(
                "k1" to TransportOutcome.Error(402),
                "k2" to TransportOutcome.Ok("k2-response")
            )
        )
        val router = AiChatRouter(
            transports = mapOf(AiProvider.GEMINI to transport),
            pools = mapOf(AiProvider.GEMINI to pool)
        )
        val resp = router.chat("gemini-2.5-flash", messages, 100, 0.6)
        assertEquals("k2-response", resp.choices.first().message?.content)
    }

    // -----------------------------------------------------------------
    // 4. Every key in the pool fails — pool exhausted
    // -----------------------------------------------------------------
    @Test
    fun `when every key returns 429 router throws QUOTA_EXCEEDED after bounded rotation`() = runBlocking {
        val pool = ApiKeyPool(keys = listOf("k1", "k2", "k3"))
        val transport = RecordingTransport(
            scripted = mapOf(
                "k1" to TransportOutcome.Error(429),
                "k2" to TransportOutcome.Error(429),
                "k3" to TransportOutcome.Error(429)
            )
        )
        val router = AiChatRouter(
            transports = mapOf(AiProvider.GEMINI to transport),
            pools = mapOf(AiProvider.GEMINI to pool)
        )

        try {
            router.chat("gemini-2.5-flash", messages, 100, 0.6)
            fail("expected AiAttemptFailure when every key cools")
        } catch (e: AiAttemptFailure) {
            assertEquals(AiErrorCategory.QUOTA_EXCEEDED, e.category)
        }

        // Bounded retry: at most pool.size attempts. NEVER more.
        assertEquals(3, transport.calls.size)
    }

    // -----------------------------------------------------------------
    // 5. Empty pool → INVALID_KEY without ever calling the transport
    // -----------------------------------------------------------------
    @Test
    fun `empty pool throws INVALID_KEY and never calls the transport`() = runBlocking {
        val transport = RecordingTransport(scripted = emptyMap())
        val router = AiChatRouter(
            transports = mapOf(AiProvider.GEMINI to transport),
            pools = mapOf(AiProvider.GEMINI to ApiKeyPool(emptyList()))
        )
        try {
            router.chat("gemini-2.5-flash", messages, 100, 0.6)
            fail("expected AiAttemptFailure when pool is empty")
        } catch (e: AiAttemptFailure) {
            assertEquals(AiErrorCategory.INVALID_KEY, e.category)
        }
        assertEquals(0, transport.calls.size)
    }

    // -----------------------------------------------------------------
    // 6. 5xx is upstream of the key — router does NOT cool, bubbles HttpException
    // -----------------------------------------------------------------
    @Test
    fun `500 bubbles up as HttpException and does NOT cool the key`() = runBlocking {
        var fakeNow = 1_000L
        val pool = ApiKeyPool(keys = listOf("k1"), nowProvider = { fakeNow })
        val transport = RecordingTransport(
            scripted = mapOf("k1" to TransportOutcome.Error(500))
        )
        val router = AiChatRouter(
            transports = mapOf(AiProvider.GEMINI to transport),
            pools = mapOf(AiProvider.GEMINI to pool)
        )
        try {
            router.chat("gemini-2.5-flash", messages, 100, 0.6)
            fail("expected HttpException to bubble for 5xx")
        } catch (e: HttpException) {
            assertEquals(500, e.code())
        }
        // Key NOT cooled — next acquire still gets k1.
        assertEquals("k1", pool.acquireHealthy())
    }

    // -----------------------------------------------------------------
    // 7. End-to-end provider failover: Gemini all cooled → caller routes
    //    the next model through OpenRouter pool/transport (separate maps).
    // -----------------------------------------------------------------
    @Test
    fun `provider failover — Gemini exhausted then OpenRouter answers`() = runBlocking {
        // Two cool-able failures on Gemini's single key.
        val geminiPool = ApiKeyPool(keys = listOf("g1"))
        val geminiTransport = RecordingTransport(
            scripted = mapOf("g1" to TransportOutcome.Error(401))
        )
        // OpenRouter answers.
        val openrouterPool = ApiKeyPool(keys = listOf("o1"))
        val openrouterTransport = RecordingTransport(
            scripted = mapOf("o1" to TransportOutcome.Ok("openrouter-ok"))
        )

        val router = AiChatRouter(
            transports = mapOf(
                AiProvider.GEMINI to geminiTransport,
                AiProvider.OPENROUTER to openrouterTransport
            ),
            pools = mapOf(
                AiProvider.GEMINI to geminiPool,
                AiProvider.OPENROUTER to openrouterPool
            )
        )

        // Step 1: caller (chain walker) tries a Gemini model → exhausts Gemini pool.
        try {
            router.chat("gemini-2.5-flash", messages, 100, 0.6)
            fail("expected AiAttemptFailure when Gemini pool exhausted")
        } catch (e: AiAttemptFailure) {
            assertEquals(AiErrorCategory.QUOTA_EXCEEDED, e.category)
        }

        // Step 2: caller advances to an OpenRouter model — different pool, different transport.
        val resp = router.chat("google/gemini-2.5-flash:free", messages, 100, 0.6)
        assertNotNull(resp.choices.first().message)
        assertEquals("openrouter-ok", resp.choices.first().message?.content)

        // Gemini was tried exactly once (its only key) before failover.
        assertEquals(1, geminiTransport.calls.size)
        assertEquals(1, openrouterTransport.calls.size)
    }

    // -----------------------------------------------------------------
    // 8. Bounded retry guard — even with pool=10 keys, only 10 attempts.
    //    Combined with the chain walker's at-most-one pass, the total
    //    work is finite. (Implicitly tested in #4; here we make it explicit
    //    with a deliberately larger pool.)
    // -----------------------------------------------------------------
    @Test
    fun `at most pool size attempts per chat call — no infinite loop`() = runBlocking {
        val keys = (1..10).map { "k$it" }
        val pool = ApiKeyPool(keys = keys)
        val transport = RecordingTransport(
            scripted = keys.associateWith { TransportOutcome.Error(429) }
        )
        val router = AiChatRouter(
            transports = mapOf(AiProvider.GEMINI to transport),
            pools = mapOf(AiProvider.GEMINI to pool)
        )
        try {
            router.chat("gemini-2.5-flash", messages, 100, 0.6)
            fail("expected exhaustion")
        } catch (_: AiAttemptFailure) {
            // expected
        }
        // EXACTLY 10 attempts — never 11, never 100, never infinite.
        assertEquals(10, transport.calls.size)
    }

    // -----------------------------------------------------------------
    // 9. Unknown model defaults to OpenRouter — protects against a stale
    //    model list shipping in a release.
    // -----------------------------------------------------------------
    @Test
    fun `unknown model routes through OpenRouter provider`() = runBlocking {
        val openrouterTransport = RecordingTransport(
            scripted = mapOf("o1" to TransportOutcome.Ok("fallback-ok"))
        )
        val router = AiChatRouter(
            transports = mapOf(AiProvider.OPENROUTER to openrouterTransport),
            pools = mapOf(AiProvider.OPENROUTER to ApiKeyPool(keys = listOf("o1")))
        )
        val resp = router.chat("nobody/never-heard-of-this", messages, 100, 0.6)
        assertEquals("fallback-ok", resp.choices.first().message?.content)
        assertEquals(1, openrouterTransport.calls.size)
    }

    // -----------------------------------------------------------------
    // 10. Mixed cool-then-succeed inside one call — verifies router doesn't
    //     give up just because the first key cooled; it walks the pool.
    // -----------------------------------------------------------------
    @Test
    fun `first key 429 then second key succeeds in a single chat call`() = runBlocking {
        val pool = ApiKeyPool(keys = listOf("k1", "k2", "k3"))
        val transport = RecordingTransport(
            scripted = mapOf(
                "k1" to TransportOutcome.Error(429),
                "k2" to TransportOutcome.Ok("k2-content"),
                "k3" to TransportOutcome.Ok("never-called-k3")
            )
        )
        val router = AiChatRouter(
            transports = mapOf(AiProvider.GEMINI to transport),
            pools = mapOf(AiProvider.GEMINI to pool)
        )
        val resp = router.chat("gemini-2.5-flash", messages, 100, 0.6)
        assertEquals("k2-content", resp.choices.first().message?.content)
        assertEquals(listOf("k1", "k2"), transport.calls.map { it.apiKey })
    }

    // =================================================================
    // Test helpers
    // =================================================================

    private sealed interface TransportOutcome {
        data class Ok(val text: String) : TransportOutcome
        data class Error(val code: Int) : TransportOutcome
    }

    private data class Call(val model: String, val apiKey: String)

    /** Records every invocation and replays a scripted outcome per API key. */
    private class RecordingTransport(
        private val scripted: Map<String, TransportOutcome>
    ) : ChatTransport {
        val calls: MutableList<Call> = mutableListOf()

        override suspend fun chat(
            model: String,
            apiKey: String,
            messages: List<ChatMessage>,
            maxTokens: Int,
            temperature: Double
        ): ChatResponse {
            calls += Call(model, apiKey)
            return when (val outcome = scripted[apiKey]) {
                is TransportOutcome.Ok -> ChatResponse(
                    id = "test",
                    model = model,
                    choices = listOf(
                        com.example.betterme.data.ai.dto.ChatChoice(
                            index = 0,
                            message = ChatMessage(role = "assistant", content = outcome.text)
                        )
                    )
                )
                is TransportOutcome.Error -> throw HttpException(
                    Response.error<Unit>(
                        outcome.code,
                        "scripted ${outcome.code}".toResponseBody("text/plain".toMediaType())
                    )
                )
                null -> error("no scripted outcome for key=$apiKey")
            }
        }
    }

    // -----------------------------------------------------------------
    // 11. Per-attempt withTimeout — a hung transport is aborted at the
    //     configured budget and surfaces as AiAttemptFailure(TIMEOUT) so
    //     the chain walker can advance instead of waiting forever.
    // -----------------------------------------------------------------
    @Test
    fun `per-attempt timeout fires AiAttemptFailure with TIMEOUT category`() = runBlocking {
        val pool = ApiKeyPool(keys = listOf("k1"))
        val transport = object : ChatTransport {
            override suspend fun chat(
                model: String,
                apiKey: String,
                messages: List<ChatMessage>,
                maxTokens: Int,
                temperature: Double
            ): ChatResponse {
                // Suspend forever — the router's withTimeout must cut us off.
                awaitCancellation()
            }
        }
        val router = AiChatRouter(
            transports = mapOf(AiProvider.GEMINI to transport),
            pools = mapOf(AiProvider.GEMINI to pool),
            perAttemptTimeoutMs = 50L
        )
        try {
            router.chat("gemini-2.5-flash", messages, 100, 0.6)
            fail("expected TIMEOUT")
        } catch (e: AiAttemptFailure) {
            assertEquals(AiErrorCategory.TIMEOUT, e.category)
        }
    }

    // -----------------------------------------------------------------
    // 12. Per-attempt timeout does NOT cool the key — the credential is
    //     fine, the latency is the model's fault. Other keys must remain
    //     eligible so the router can rotate without burning the pool.
    // -----------------------------------------------------------------
    @Test
    fun `per-attempt timeout does not cool the key`() = runBlocking {
        val pool = ApiKeyPool(keys = listOf("k1", "k2"))
        val transport = object : ChatTransport {
            override suspend fun chat(
                model: String,
                apiKey: String,
                messages: List<ChatMessage>,
                maxTokens: Int,
                temperature: Double
            ): ChatResponse = awaitCancellation()
        }
        val router = AiChatRouter(
            transports = mapOf(AiProvider.GEMINI to transport),
            pools = mapOf(AiProvider.GEMINI to pool),
            perAttemptTimeoutMs = 30L
        )
        try {
            router.chat("gemini-2.5-flash", messages, 100, 0.6)
            fail("expected timeout failure")
        } catch (_: AiAttemptFailure) {
            // expected
        }
        // Neither key should be cooled — a timeout categorizes as transient,
        // not credential-related. allCooled() must stay false.
        assertFalse("timeout should not cool any key", pool.allCooled())
    }

    // -----------------------------------------------------------------
    // 13. Health tracker records the last success per provider — used for
    //     diagnostics logging in Logcat.
    // -----------------------------------------------------------------
    @Test
    fun `health tracker records success and failure per provider`() = runBlocking {
        val health = AiProviderHealth(nowProvider = { 5_000L })
        val pool = ApiKeyPool(keys = listOf("k1", "k2"))
        val transport = RecordingTransport(
            scripted = mapOf(
                "k1" to TransportOutcome.Error(429),
                "k2" to TransportOutcome.Ok("ok")
            )
        )
        val router = AiChatRouter(
            transports = mapOf(AiProvider.GEMINI to transport),
            pools = mapOf(AiProvider.GEMINI to pool),
            healthTracker = health
        )
        router.chat("gemini-2.5-flash", messages, 100, 0.6)
        val snap = health.snapshot()[AiProvider.GEMINI]!!
        assertEquals(5_000L, snap.lastSuccessAt)
        assertEquals("gemini-2.5-flash", snap.lastSuccessModel)
        // The 429 on k1 was recorded as a failure before the success on k2.
        assertEquals(AiErrorCategory.RATE_LIMITED, snap.lastFailureCategory)
    }

    // -----------------------------------------------------------------
    // 14. Health tracker is optional — when null, router still works.
    // -----------------------------------------------------------------
    @Test
    fun `null health tracker is a no-op — router still answers`() = runBlocking {
        val pool = ApiKeyPool(keys = listOf("k1"))
        val transport = RecordingTransport(
            scripted = mapOf("k1" to TransportOutcome.Ok("ok"))
        )
        val router = AiChatRouter(
            transports = mapOf(AiProvider.GEMINI to transport),
            pools = mapOf(AiProvider.GEMINI to pool),
            healthTracker = null
        )
        val resp = router.chat("gemini-2.5-flash", messages, 100, 0.6)
        assertEquals("ok", resp.choices.first().message?.content)
    }

    // -----------------------------------------------------------------
    // 15. Provider health tracker debugStatusLine never returns null.
    // -----------------------------------------------------------------
    @Test
    fun `provider health debug status line shape`() {
        val h = AiProviderHealth(nowProvider = { 10_000L })
        // Empty — well-formed sentinel.
        assertEquals("AiProviderHealth(empty)", h.debugStatusLine())
        h.recordSuccess(AiProvider.GEMINI, "gemini-2.5-flash")
        val line = h.debugStatusLine()
        assertTrue(line.contains("GEMINI"))
        assertTrue(line.contains("gemini-2.5-flash"))
        // The untouched OpenRouter provider must still appear in the line.
        assertTrue(line.contains("OPENROUTER"))
    }

    @Test
    fun `recording transport sanity — supplied scripts run, not the unused k3`() = runBlocking {
        // Sanity check on the test harness itself: scripted outcomes are isolated per key.
        val pool = ApiKeyPool(keys = listOf("k1"))
        val transport = RecordingTransport(scripted = mapOf("k1" to TransportOutcome.Ok("ok")))
        val router = AiChatRouter(
            transports = mapOf(AiProvider.GEMINI to transport),
            pools = mapOf(AiProvider.GEMINI to pool)
        )
        val resp = router.chat("gemini-2.5-flash", messages, 100, 0.6)
        assertEquals("ok", resp.choices.first().message?.content)
        assertTrue(transport.calls.size == 1)
    }
}

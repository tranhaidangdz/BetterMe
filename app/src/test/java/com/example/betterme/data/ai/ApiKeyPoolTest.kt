package com.example.betterme.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract for [ApiKeyPool]:
 *
 *  - empty pool → never hands out a key
 *  - blank keys are filtered out at construction so an empty CSV entry doesn't
 *    silently become a `Bearer ""` request
 *  - acquireHealthy round-robins so requests spread evenly under load
 *  - cooled keys skipped until their cooldown window elapses
 *  - all keys cooled → returns null (router treats as provider-exhausted)
 *  - cooldown(unknownKey) is a no-op (parallel rotation safety)
 *  - debugStatusLine masks every key it logs
 */
class ApiKeyPoolTest {

    @Test
    fun `empty pool acquires nothing and reports empty`() {
        val pool = ApiKeyPool(keys = emptyList())
        assertTrue(pool.isEmpty)
        assertEquals(0, pool.size)
        assertNull(pool.acquireHealthy())
    }

    @Test
    fun `blank keys are stripped at construction`() {
        val pool = ApiKeyPool(keys = listOf("", "  ", "real-key", " "))
        assertEquals(1, pool.size)
        assertEquals("real-key", pool.acquireHealthy())
    }

    @Test
    fun `acquireHealthy is round-robin across multiple keys`() {
        val pool = ApiKeyPool(keys = listOf("k1", "k2", "k3"))
        // Three sequential acquires should hit each key exactly once before repeating.
        val seq = (1..6).map { pool.acquireHealthy() }
        assertEquals(listOf("k1", "k2", "k3", "k1", "k2", "k3"), seq)
    }

    @Test
    fun `cooled key is skipped until cooldown elapses`() {
        var fakeNow = 1_000L
        val pool = ApiKeyPool(keys = listOf("k1", "k2"), nowProvider = { fakeNow })

        // Acquire k1, mark it cooled for 5s — next acquire should hit k2.
        assertEquals("k1", pool.acquireHealthy())
        pool.cooldown("k1", 5_000L)

        // Walk past the cursor a few times — k1 must stay skipped.
        assertEquals("k2", pool.acquireHealthy())
        assertEquals("k2", pool.acquireHealthy())
        assertEquals("k2", pool.acquireHealthy())

        // Advance time to just-after the cooldown window — k1 is eligible again.
        fakeNow += 5_001L
        // Cursor sat at k2 from the prior calls; next acquire walks to k1.
        assertEquals("k1", pool.acquireHealthy())
    }

    @Test
    fun `all keys cooled returns null`() {
        var fakeNow = 1_000L
        val pool = ApiKeyPool(keys = listOf("k1", "k2"), nowProvider = { fakeNow })
        pool.cooldown("k1", 10_000L)
        pool.cooldown("k2", 10_000L)
        assertNull(pool.acquireHealthy())
        assertTrue(pool.allCooled())

        // After cooldown elapses, eligible again.
        fakeNow += 10_001L
        assertFalse(pool.allCooled())
        assertNotNull(pool.acquireHealthy())
    }

    @Test
    fun `cooldown with unknown key is a noop`() {
        val pool = ApiKeyPool(keys = listOf("k1"))
        // Should not throw, should not mark anything cooled.
        pool.cooldown("does-not-exist", 999_999L)
        assertEquals("k1", pool.acquireHealthy())
    }

    @Test
    fun `cooldown with non-positive duration is a noop`() {
        var fakeNow = 1_000L
        val pool = ApiKeyPool(keys = listOf("k1"), nowProvider = { fakeNow })
        pool.cooldown("k1", 0L)
        pool.cooldown("k1", -500L)
        assertEquals("k1", pool.acquireHealthy())
    }

    @Test
    fun `debugStatusLine masks every key`() {
        val pool = ApiKeyPool(keys = listOf("AIzaSyA1B2C3D4E5F6G7H8I9J0", "sk-or-v1-abcdef1234567890"))
        val line = pool.debugStatusLine()
        // Each raw key must NOT appear in full.
        assertFalse("status leaked raw key", line.contains("AIzaSyA1B2C3D4E5F6G7H8I9J0"))
        assertFalse("status leaked raw key", line.contains("sk-or-v1-abcdef1234567890"))
        // Masked form should be present (prefix + …).
        assertTrue("masked prefix missing for k1", line.contains("AIzaSy"))
        assertTrue("masked prefix missing for k2", line.contains("sk-or-"))
    }

    @Test
    fun `mask handles short and blank keys without leaking`() {
        assertEquals("(blank)", ApiKeyPool.mask(""))
        assertEquals("(blank)", ApiKeyPool.mask("   "))
        assertEquals("***", ApiKeyPool.mask("short"))
        assertEquals("AIzaSy…ZY", ApiKeyPool.mask("AIzaSy123456789ZY"))
    }
}

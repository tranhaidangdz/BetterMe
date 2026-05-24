package com.example.betterme.data.ai

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Behavioral tests for [SingleFlight]. Uses [runBlocking] + launched coroutines
 * to verify the contract without pulling in `kotlinx-coroutines-test`.
 *
 * Each test exercises one scenario the AI repo relies on:
 *  - identical concurrent callers fire the block once
 *  - different keys fire independent blocks
 *  - map cleans up after completion so the next call isn't stale-joined
 *  - failures propagate to every waiting caller
 *  - keys with no contention behave as a normal suspending call
 */
class SingleFlightTest {

    @Test
    fun `same key concurrent callers share one block invocation`() = runBlocking {
        val sf = SingleFlight()
        val invocations = AtomicInteger(0)
        val block: suspend () -> String = {
            invocations.incrementAndGet()
            delay(40) // hold long enough for the second caller to find the in-flight entry
            "result"
        }

        val a = async { sf.run("key-1") { block() } }
        // Tiny delay so caller A definitely registers before caller B asks.
        delay(5)
        val b = async { sf.run("key-1") { block() } }

        assertEquals(listOf("result", "result"), awaitAll(a, b))
        assertEquals(1, invocations.get())
    }

    @Test
    fun `different keys fire independent blocks`() = runBlocking {
        val sf = SingleFlight()
        val invocations = AtomicInteger(0)

        val a = async { sf.run("alpha") { invocations.incrementAndGet(); delay(20); "a" } }
        val b = async { sf.run("beta") { invocations.incrementAndGet(); delay(20); "b" } }

        assertEquals(listOf("a", "b"), awaitAll(a, b))
        assertEquals(2, invocations.get())
    }

    @Test
    fun `map clears after completion so next call runs again`() = runBlocking {
        val sf = SingleFlight()
        val invocations = AtomicInteger(0)

        repeat(3) {
            sf.run<String>("re-entry") {
                invocations.incrementAndGet()
                "ok"
            }
        }

        // Three sequential calls → three independent invocations (no stale join).
        assertEquals(3, invocations.get())
        assertEquals(0, sf.inFlightCount())
    }

    @Test
    fun `failure propagates to every joined caller`() = runBlocking {
        val sf = SingleFlight()
        val a = async {
            runCatching {
                sf.run<String>("fail-key") {
                    delay(20)
                    throw IllegalStateException("boom")
                }
            }
        }
        delay(5)
        val b = async {
            runCatching {
                sf.run<String>("fail-key") {
                    // This block should never execute because A's deferred is in-flight.
                    "should-not-run"
                }
            }
        }

        val aResult = a.await()
        val bResult = b.await()
        assertTrue(aResult.isFailure)
        assertTrue(bResult.isFailure)
        assertEquals("boom", aResult.exceptionOrNull()?.message)
        assertEquals("boom", bResult.exceptionOrNull()?.message)
        // Map cleaned up even on failure.
        assertEquals(0, sf.inFlightCount())
    }

    @Test
    fun `solo call returns the block result normally`() = runBlocking {
        val sf = SingleFlight()
        val r = sf.run("only") { 42 }
        assertEquals(42, r)
        assertEquals(0, sf.inFlightCount())
    }

    @Test
    fun `type erasure works across heterogeneous keys`() = runBlocking {
        val sf = SingleFlight()
        val intResult: Int = sf.run("k-int") { 7 }
        val stringResult: String = sf.run("k-str") { "hello" }
        assertEquals(7, intResult)
        assertEquals("hello", stringResult)
    }

    @Test
    fun `in-flight count visible while a call is running`() = runBlocking {
        val sf = SingleFlight()
        var seenCount: Int? = null
        val inner = async {
            sf.run<String>("watcher") {
                // Snapshot the count from a sibling coroutine while we sleep.
                delay(30)
                "done"
            }
        }
        delay(10)
        seenCount = sf.inFlightCount()
        inner.await()
        assertNotNull(seenCount)
        assertEquals(1, seenCount)
        assertEquals(0, sf.inFlightCount())
    }
}

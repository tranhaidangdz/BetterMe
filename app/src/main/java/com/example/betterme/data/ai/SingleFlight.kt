package com.example.betterme.data.ai

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Tiny single-flight helper. Concurrent callers that pass the same `key` collapse
 * onto a single in-flight [Deferred] — only the first actually invokes [block];
 * everyone else awaits the same result.
 *
 * The internal scope is a process-singleton `SupervisorJob + Dispatchers.IO`,
 * intentionally independent of any caller's coroutine. A caller cancelling its
 * own coroutine therefore does NOT abort the in-flight HTTP work — other
 * awaiters still get the result. That trade-off is the right one for AI
 * requests: the bytes are already in flight, and cancelling the OkHttp call
 * just wastes the work.
 *
 * Memory safety: the map entry is removed inside the `finally` of the async
 * body, so completed / failed / cancelled entries don't leak.
 *
 * Type-erasure note: the internal map stores `Deferred<Any?>`, with a checked
 * cast on `await()`. The contract is that callers using the same `key` also
 * expect the same return type `V` — this is enforced by convention (use unique
 * key prefixes per call site, e.g. `"lifestyle:$promptHash"`).
 */
class SingleFlight {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val inFlight = mutableMapOf<String, Deferred<Any?>>()

    @Suppress("UNCHECKED_CAST")
    suspend fun <V> run(key: String, block: suspend () -> V): V {
        val deferred: Deferred<Any?> = mutex.withLock {
            inFlight[key]?.also {
                if (METRICS) Log.d(TAG, "dedup hit key=$key — joining existing in-flight call")
            } ?: scope.async {
                try {
                    block() as Any?
                } finally {
                    mutex.withLock { inFlight.remove(key) }
                }
            }.also { inFlight[key] = it }
        }
        return deferred.await() as V
    }

    /** Active in-flight key count. Exposed for diagnostics; never load-bearing. */
    suspend fun inFlightCount(): Int = mutex.withLock { inFlight.size }

    private companion object {
        const val TAG = "AiSingleFlight"
        const val METRICS = true
    }
}

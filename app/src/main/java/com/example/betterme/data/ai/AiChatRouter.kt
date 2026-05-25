package com.example.betterme.data.ai

import android.util.Log
import com.example.betterme.BuildConfig
import com.example.betterme.data.ai.dto.ChatMessage
import com.example.betterme.data.ai.dto.ChatResponse
import com.example.betterme.domain.ai.AiErrorCategory
import retrofit2.HttpException

/**
 * Per-call provider + key dispatcher. The repository's chain walker iterates
 * model slugs in [AiProvider.FALLBACK_MODELS] (Gemini first, OpenRouter
 * fallback) and asks the router to execute each one; the router resolves the
 * owning provider, picks a healthy key from its pool, and either:
 *
 *  - returns a normalized [ChatResponse] on success, or
 *  - cools the key and rotates to the next one when the failure was credential-
 *    or quota-related (HTTP 401/402/403/429), bounded by `pool.size` attempts
 *    so a fully-cooled provider falls through to the chain walker fast, or
 *  - rethrows when the failure was upstream of the credential (timeout,
 *    network, 5xx, 4xx that aren't auth/quota/rate) so the chain walker can
 *    advance to the next model.
 *
 * Cooldown policy (defaults override-able via constructor for tests):
 *  - RATE_LIMITED (429)              → 60 s   — token-bucket likely refilling.
 *  - INVALID_KEY (401/403) /
 *    QUOTA_EXCEEDED (402, RESOURCE_EXHAUSTED)
 *                                    → 10 min — likely a longer outage.
 *  - Anything else                   → no cooldown, bubble up.
 *
 * Bounded retry: a single `chat()` call makes at most `pool.size` attempts for
 * one model. Combined with the chain walker's at-most-one pass over
 * `FALLBACK_MODELS`, the worst case is `sum_over_providers(pool.size * models)`
 * attempts before [AiUnavailableException] surfaces. No infinite loops.
 */
class AiChatRouter(
    private val transports: Map<AiProvider, ChatTransport>,
    private val pools: Map<AiProvider, ApiKeyPool>,
    private val rateLimitCooldownMs: Long = DEFAULT_RATE_LIMIT_COOLDOWN_MS,
    private val authQuotaCooldownMs: Long = DEFAULT_AUTH_QUOTA_COOLDOWN_MS
) {

    /**
     * Execute one chat call for [model]. Throws [AiAttemptFailure] when the
     * model's provider is unconfigured or fully cooled; rethrows HTTP/network
     * exceptions verbatim when the failure isn't a key issue so the chain
     * walker can categorize the same way it always has.
     */
    suspend fun chat(
        model: String,
        messages: List<ChatMessage>,
        maxTokens: Int,
        temperature: Double
    ): ChatResponse {
        val provider = AiProvider.providerFor(model)
        val pool = pools[provider]
            ?: throw AiAttemptFailure(
                AiErrorCategory.INVALID_KEY,
                "No API key pool configured for provider $provider"
            )
        val transport = transports[provider]
            ?: throw AiAttemptFailure(
                AiErrorCategory.MODEL_UNAVAILABLE,
                "No transport configured for provider $provider"
            )

        if (pool.isEmpty) {
            throw AiAttemptFailure(
                AiErrorCategory.INVALID_KEY,
                "No API keys configured for provider $provider"
            )
        }

        val keyCap = pool.size
        repeat(keyCap) { attemptIdx ->
            val key = pool.acquireHealthy()
                ?: throw AiAttemptFailure(
                    AiErrorCategory.QUOTA_EXCEEDED,
                    "All $keyCap keys for $provider are cooled"
                )

            try {
                if (BuildConfig.DEBUG) {
                    Log.d(
                        TAG,
                        "→ $provider model=$model attempt=${attemptIdx + 1}/$keyCap " +
                            "key=${ApiKeyPool.mask(key)}"
                    )
                }
                return transport.chat(model, key, messages, maxTokens, temperature)
            } catch (e: HttpException) {
                val category = AiErrorCategorizer.categorizeHttp(e.code())
                val cooldown = cooldownFor(category)
                if (cooldown > 0L) {
                    pool.cooldown(key, cooldown)
                    Log.w(
                        TAG,
                        "× $provider model=$model key=${ApiKeyPool.mask(key)} " +
                            "code=${e.code()} → category=$category " +
                            "cooled for ${cooldown}ms, rotating to next key"
                    )
                    // try next key for the SAME model
                } else {
                    // Not a credential issue — let the chain walker decide
                    // whether to advance to the next model.
                    throw e
                }
            }
        }

        throw AiAttemptFailure(
            AiErrorCategory.QUOTA_EXCEEDED,
            "All $keyCap keys for $provider exhausted after rotation"
        )
    }

    private fun cooldownFor(category: AiErrorCategory): Long = when (category) {
        AiErrorCategory.RATE_LIMITED -> rateLimitCooldownMs
        AiErrorCategory.INVALID_KEY, AiErrorCategory.QUOTA_EXCEEDED -> authQuotaCooldownMs
        else -> 0L
    }

    companion object {
        private const val TAG = "AiChatRouter"

        /** 1 minute — typical token-bucket refill horizon on free tiers. */
        const val DEFAULT_RATE_LIMIT_COOLDOWN_MS: Long = 60_000L

        /** 10 minutes — likely a longer outage (revoked key, daily quota). */
        const val DEFAULT_AUTH_QUOTA_COOLDOWN_MS: Long = 600_000L
    }
}

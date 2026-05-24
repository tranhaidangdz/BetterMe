package com.example.betterme.data.ai

import com.example.betterme.domain.ai.AiErrorCategory
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Pure helpers that map exceptions / HTTP status codes / model-chain outcomes into
 * [AiErrorCategory] values. Centralized so every per-model attempt and every chain
 * walker categorizes the same way.
 *
 * The split between this object and `AiHabitInsightRepositoryImpl.extractHttpErrorMessage`
 * is intentional: messages stay user-facing Vietnamese; categories stay
 * machine-readable. ViewModels typically consume the category and pick localized copy
 * via [com.example.betterme.domain.ai.AiUnavailableException.userMessage].
 */
internal object AiErrorCategorizer {

    /**
     * Map a single thrown exception captured inside a per-model attempt to a
     * category. Used by the per-model try functions; callers wrap the result in
     * [AiAttemptFailure] and let the chain walker aggregate.
     */
    fun categorize(t: Throwable): AiErrorCategory = when (t) {
        is SocketTimeoutException -> AiErrorCategory.TIMEOUT
        is UnknownHostException -> AiErrorCategory.NO_NETWORK
        is HttpException -> categorizeHttp(t.code())
        is SerializationException -> AiErrorCategory.PARSE
        is IOException -> AiErrorCategory.NO_NETWORK
        else -> AiErrorCategory.UNKNOWN
    }

    fun categorizeHttp(code: Int): AiErrorCategory = when (code) {
        401 -> AiErrorCategory.INVALID_KEY
        402 -> AiErrorCategory.QUOTA_EXCEEDED
        403 -> AiErrorCategory.INVALID_KEY // OpenRouter returns 403 for forbidden / disabled keys
        404, 400 -> AiErrorCategory.MODEL_UNAVAILABLE
        429 -> AiErrorCategory.RATE_LIMITED
        in 500..599 -> AiErrorCategory.SERVER_ERROR
        else -> AiErrorCategory.UNKNOWN
    }

    /**
     * When every model in the chain fails, pick which category to surface to the
     * user. Priority: terminal causes (INVALID_KEY, QUOTA_EXCEEDED) come first
     * because retrying won't help; transient causes (RATE_LIMITED, SERVER_ERROR)
     * are next; categorization noise (MODEL_UNAVAILABLE → stale list) is below
     * those; UNKNOWN is the floor.
     *
     * Empty list → UNKNOWN.
     */
    fun pickWorst(categories: List<AiErrorCategory>): AiErrorCategory {
        if (categories.isEmpty()) return AiErrorCategory.UNKNOWN
        val rank = listOf(
            AiErrorCategory.INVALID_KEY,
            AiErrorCategory.QUOTA_EXCEEDED,
            AiErrorCategory.NO_NETWORK,
            AiErrorCategory.TIMEOUT,
            AiErrorCategory.RATE_LIMITED,
            AiErrorCategory.SERVER_ERROR,
            AiErrorCategory.PARSE,
            AiErrorCategory.MODEL_UNAVAILABLE,
            AiErrorCategory.UNKNOWN
        )
        return rank.firstOrNull { it in categories } ?: AiErrorCategory.UNKNOWN
    }
}

/**
 * Carries the category + detail from a single failed model attempt up to the
 * chain walker. Kept internal so callers outside the AI repo never have to
 * reason about per-attempt failures.
 */
internal class AiAttemptFailure(
    val category: AiErrorCategory,
    val detail: String
) : Exception(detail)

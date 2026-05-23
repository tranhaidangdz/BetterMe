package com.example.betterme.domain.ai

/**
 * Thrown by the AI repo when every model in the OpenRouter fallback chain fails
 * (network down, rate-limited, parse error, empty response). Use cases that previously
 * received a `isCanned = true` deterministic fallback now propagate this exception so
 * the UI can render a proper retry-able error state instead of a hardcoded canned card
 * disguised as a real AI response.
 */
class AiUnavailableException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

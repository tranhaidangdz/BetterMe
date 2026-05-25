package com.example.betterme.data.ai

/**
 * Declared providers for chat-completion calls. Each provider owns its own model
 * list and its own key pool; the router tries providers in [ORDERED] order and,
 * within each provider, walks the per-provider [models] list.
 *
 * The "Gemini-first then OpenRouter" order is intentional. Native Gemini usually
 * has lower latency and a more generous quota per Google account than
 * OpenRouter's `:free` aggregator tier, so when both have healthy keys we want
 * Gemini to absorb the request. OpenRouter remains the deeper fallback because
 * its catalog spans multiple upstream providers (DeepSeek, Qwen, Llama, Mistral)
 * and thus survives outages of any single upstream.
 *
 * Adding a new provider (Together, Groq, Fireworks, DeepInfra…) is a 3-step
 * change: add an enum entry with its model list, add a [ChatTransport] impl,
 * and wire a pool + transport in the Koin module. The router needs no edits.
 */
enum class AiProvider(val models: List<String>) {
    /**
     * Native Google Gemini API (generativelanguage.googleapis.com). Model names
     * are the bare slugs — no `google/` prefix, no `:free` suffix. Quotas are
     * per Google Cloud project, so multiple API keys from different projects
     * effectively multiply throughput before any rotation.
     */
    GEMINI(
        models = listOf(
            "gemini-2.5-flash",
            "gemini-2.0-flash"
        )
    ),

    /**
     * OpenRouter aggregator. Model names include the upstream provider prefix
     * and the `:free` tier suffix. The chain is latency-first then
     * provider-diversified so a single upstream rate-limit can't stall all
     * seven entries.
     */
    OPENROUTER(
        models = listOf(
            "google/gemini-2.5-flash:free",
            "deepseek/deepseek-chat-v3-0324:free",
            "qwen/qwen-2.5-72b-instruct:free",
            "meta-llama/llama-3.3-70b-instruct:free",
            "google/gemma-2-9b-it:free",
            "mistralai/mistral-small-3.2-24b-instruct:free",
            "google/gemini-2.0-flash-exp:free"
        )
    );

    companion object {
        /** Try-order. Gemini first, OpenRouter fallback. */
        val ORDERED: List<AiProvider> = listOf(GEMINI, OPENROUTER)

        /**
         * Flat fallback chain — Gemini models first, then OpenRouter models —
         * used by [AiHabitInsightRepositoryImpl] as a drop-in for the legacy
         * `FALLBACK_MODELS` constant. Order preserved across cold start.
         */
        val FALLBACK_MODELS: List<String> = ORDERED.flatMap { it.models }

        private val MODEL_TO_PROVIDER: Map<String, AiProvider> =
            ORDERED.flatMap { p -> p.models.map { it to p } }.toMap()

        /**
         * Lookup the provider that owns a model slug. Falls back to OPENROUTER
         * for unknown models so a stale model list can't crash the router —
         * the call will surface a 404 MODEL_UNAVAILABLE through the normal
         * chain walker, matching legacy behavior.
         */
        fun providerFor(model: String): AiProvider =
            MODEL_TO_PROVIDER[model] ?: OPENROUTER
    }
}

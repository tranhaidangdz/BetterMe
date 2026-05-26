package com.example.betterme.data.ai

/**
 * Declared providers for chat-completion calls. As of the Gemini-only migration
 * there is exactly one — [GEMINI] — but the enum + [ChatTransport] abstraction
 * is kept so adding another provider (Claude, Together, Groq…) tomorrow stays a
 * 3-step change: add an entry with its model list, add a transport, wire a pool
 * + transport in the Koin module. The router needs no edits.
 *
 * Why an enum for one value: the [AiChatRouter] is map-keyed by provider, every
 * call site already imports the enum, and the chain walker iterates
 * [FALLBACK_MODELS] not providers — collapsing to a sealed object would force a
 * cross-cutting refactor without any architectural payoff.
 */
enum class AiProvider(val models: List<String>) {
    /**
     * Native Google Gemini API (generativelanguage.googleapis.com). Model names
     * are the bare slugs — no `google/` prefix, no `:free` suffix. Quotas are
     * per Google Cloud project, so multiple API keys from different projects
     * effectively multiply throughput before any rotation.
     *
     * Chain order: `gemini-2.5-flash` first (fastest TTFT + strongest JSON on
     * the free tier), `gemini-2.0-flash` as the in-provider fallback so an
     * isolated 2.5 outage still answers without leaving Google's stack.
     */
    GEMINI(
        models = listOf(
            "gemini-2.5-flash",
            "gemini-2.0-flash"
        )
    );

    companion object {
        /** Try-order. Single-provider for now. */
        val ORDERED: List<AiProvider> = listOf(GEMINI)

        /**
         * Flat fallback chain used by [com.example.betterme.data.ai.AiHabitInsightRepositoryImpl]
         * as the canonical "tried in order" model list. Order preserved across
         * cold start.
         */
        val FALLBACK_MODELS: List<String> = ORDERED.flatMap { it.models }

        private val MODEL_TO_PROVIDER: Map<String, AiProvider> =
            ORDERED.flatMap { p -> p.models.map { it to p } }.toMap()

        /**
         * Lookup the provider that owns a model slug. Falls back to [GEMINI]
         * for unknown models so a stale model list can't crash the router —
         * the call will surface a 404 MODEL_UNAVAILABLE through the normal
         * chain walker, matching legacy behavior.
         */
        fun providerFor(model: String): AiProvider =
            MODEL_TO_PROVIDER[model] ?: GEMINI
    }
}

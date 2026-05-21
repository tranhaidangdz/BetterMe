package com.example.betterme.domain.challenge

/**
 * Canonical string status values for `user_challenges.status`. Kept as plain strings so
 * the Room column stays text-typed without a TypeConverter; reference this object instead
 * of duplicating string literals at the call site.
 *
 * Terminal states (COMPLETED, FAILED, ABANDONED) are irreversible — once written, no use
 * case should transition away from them. FAILED in particular must never flip to COMPLETED
 * even if subsequent check-ins land — see EvaluateChallengeStatusUseCase.
 */
object UserChallengeStatus {
    const val ACTIVE = "ACTIVE"
    const val UPCOMING = "UPCOMING"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
    const val ABANDONED = "ABANDONED"

    fun isTerminal(status: String): Boolean =
        status == COMPLETED || status == FAILED || status == ABANDONED
}

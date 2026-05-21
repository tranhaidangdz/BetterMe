package com.example.betterme.domain.usecase.challenge

import android.util.Log
import com.example.betterme.domain.repository.UserChallengeRepository

/**
 * Runs the strict-daily evaluator across every ACTIVE / UPCOMING user_challenge row.
 * Called once on app start so existing rows that crossed their deadline (or missed a
 * day) while the user was away get their final FAILED / COMPLETED transition before
 * the UI loads.
 *
 * Safe to run repeatedly — terminal rows are short-circuited inside the evaluator.
 * Idempotent across rapid startup re-entry (e.g., process death + Activity restart).
 */
class BackfillChallengeStatusesUseCase(
    private val userChallengeRepository: UserChallengeRepository,
    private val evaluateStatusUseCase: EvaluateChallengeStatusUseCase
) {

    suspend operator fun invoke() {
        val rows = try {
            userChallengeRepository.getAllActiveOrUpcoming()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load active rows for backfill", e)
            return
        }
        var failed = 0
        var completed = 0
        rows.forEach { row ->
            // Evaluate individually so a single row's failure (e.g., missing challenge
            // FK partner) doesn't poison the whole batch.
            try {
                when (evaluateStatusUseCase(row.id)) {
                    is EvaluateChallengeStatusUseCase.Outcome.FailedNow -> failed++
                    is EvaluateChallengeStatusUseCase.Outcome.CompletedNow -> completed++
                    else -> Unit
                }
            } catch (e: Exception) {
                Log.w(TAG, "Backfill evaluator failed for uc=${row.id}", e)
            }
        }
        if (failed > 0 || completed > 0) {
            Log.i(TAG, "Backfill: failed=$failed completed=$completed scanned=${rows.size}")
        }
    }

    private companion object {
        const val TAG = "ChallengeBackfill"
    }
}

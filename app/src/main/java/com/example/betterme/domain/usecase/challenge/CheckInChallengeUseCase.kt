package com.example.betterme.domain.usecase.challenge

import androidx.room.withTransaction
import com.example.betterme.data.local.room.database.BetterMeDatabase
import com.example.betterme.data.local.room.entities.AchievementEntity
import com.example.betterme.data.local.room.entities.ChallengeLogEntity
import com.example.betterme.domain.repository.ChallengeLogRepository
import com.example.betterme.domain.repository.ChallengeRepository
import com.example.betterme.domain.repository.GroupTeamRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.domain.usecase.leaderboard.SyncGlobalLeaderboardUseCase
import com.example.betterme.domain.usecase.leaderboard.SyncMyChallengeScoreUseCase
import com.example.betterme.utils.DateUtils

/**
 * Records a daily check-in for a UserChallenge.
 *
 * Returns:
 * - [Result.Progress] for normal check-ins → streak/progress updated.
 * - [Result.Completed] when this check-in pushes streak to target_streak → coins + badge awarded.
 * - [Result.AlreadyCheckedIn] when today already has a DONE log.
 * - [Result.Error] for any other failure.
 *
 * All work runs inside a single Room transaction.
 */
class CheckInChallengeUseCase(
    private val database: BetterMeDatabase,
    private val challengeRepository: ChallengeRepository,
    private val userChallengeRepository: UserChallengeRepository,
    private val challengeLogRepository: ChallengeLogRepository,
    private val groupTeamRepository: GroupTeamRepository,
    private val awardCompletionUseCase: AwardChallengeCompletionUseCase,
    private val syncMyChallengeScore: SyncMyChallengeScoreUseCase,
    private val syncGlobalLeaderboard: SyncGlobalLeaderboardUseCase
) {

    sealed class Result {
        data class Progress(
            val newStreak: Int,
            val progressPct: Int
        ) : Result()

        data class Completed(
            val coinsEarned: Int,
            val rewardBadge: AchievementEntity?,
            val bonusBadges: List<AchievementEntity>
        ) : Result()

        data object AlreadyCheckedIn : Result()
        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(
        userChallengeId: Int,
        note: String? = null,
        imageUri: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result {
        val txResult: Result = try {
            database.withTransaction {
                val uc = userChallengeRepository.getById(userChallengeId)
                    ?: return@withTransaction Result.Error("UserChallenge $userChallengeId not found")

                if (uc.status != "ACTIVE") {
                    return@withTransaction Result.Error("Thử thách không còn hoạt động")
                }

                val today = DateUtils.startOfDay()

                // Already checked in today?
                val existing = challengeLogRepository.getLogByDate(userChallengeId, today)
                if (existing != null && existing.status == "DONE") {
                    return@withTransaction Result.AlreadyCheckedIn
                }

                // Insert new log.
                val log = ChallengeLogEntity(
                    user_challenge_id = userChallengeId,
                    date = today,
                    status = "DONE",
                    note = note,
                    image = imageUri,
                    latitude = latitude,
                    longitude = longitude
                )
                challengeLogRepository.addLog(log)

                // Recompute streak from all DONE logs.
                val doneDates = challengeLogRepository.getDoneDates(userChallengeId)
                val newStreak = DateUtils.currentStreak(doneDates, today)
                val newBest = maxOf(uc.best_streak, newStreak)

                val challenge = challengeRepository.getById(uc.challenge_id)
                    ?: return@withTransaction Result.Error("Challenge ${uc.challenge_id} not found")

                val target = challenge.target_streak.coerceAtLeast(1)
                val pct = ((newStreak.toLong() * 100L) / target).toInt().coerceAtMost(100)

                userChallengeRepository.updateProgress(
                    id = userChallengeId,
                    currentStreak = newStreak,
                    bestStreak = newBest,
                    progressPct = pct,
                    lastCheckIn = today
                )

                // For group challenges, every check-in adds 1 coin to the team's running total
                // so the leaderboard reflects activity even mid-challenge.
                if (challenge.is_group && uc.team_id != null) {
                    groupTeamRepository.addCoinsToTeam(uc.team_id, 1)
                }

                if (newStreak >= target) {
                    // Reload the row with the just-updated streak fields, then award.
                    val updated = userChallengeRepository.getById(userChallengeId)!!
                    val award = awardCompletionUseCase(updated, challenge)
                    Result.Completed(
                        coinsEarned = award.coinsEarned,
                        rewardBadge = award.rewardBadge,
                        bonusBadges = award.bonusBadges
                    )
                } else {
                    Result.Progress(newStreak = newStreak, progressPct = pct)
                }
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Đã xảy ra lỗi không xác định")
        }

        // Fire-and-forget Firestore sync after a successful local commit.
        // Failures inside the sync use case are logged but do NOT undo the
        // check-in — the local record is the source of truth. Completion
        // bypasses the 30s write throttle because the user is about to
        // see the post-check-in celebration and expects the leaderboard
        // to be up to date.
        when (txResult) {
            is Result.Progress -> {
                runCatching { syncMyChallengeScore(userChallengeId, force = false) }
                runCatching { syncGlobalLeaderboard(force = false) }
            }
            is Result.Completed -> {
                runCatching { syncMyChallengeScore(userChallengeId, force = true) }
                // Completion bumps completedChallenges + may change
                // longestStreak — force the global sync too.
                runCatching { syncGlobalLeaderboard(force = true) }
            }
            else -> Unit
        }
        return txResult
    }
}

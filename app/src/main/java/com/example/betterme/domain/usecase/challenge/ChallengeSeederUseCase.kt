package com.example.betterme.domain.usecase.challenge

import androidx.room.withTransaction
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.database.BetterMeDatabase
import com.example.betterme.data.seed.BadgesSeed
import com.example.betterme.data.seed.ChallengesSeed
import com.example.betterme.data.seed.EliteChallengesSeed
import com.example.betterme.data.seed.GroupTeamsSeed
import com.example.betterme.data.seed.UpcomingChallengesSeed
import com.example.betterme.domain.repository.AchievementRepository
import com.example.betterme.domain.repository.ChallengeRepository
import com.example.betterme.domain.repository.GroupTeamRepository

/**
 * Seeds the challenge catalog, badge catalog, and mock group teams on first launch
 * (and after destructive Room migrations). Idempotent via [DataStoreManager.isChallengesSeeded].
 *
 * Order matters: badges first (challenges FK to reward_badge_id), then challenges, then teams
 * (teams FK to challenge_id).
 */
class ChallengeSeederUseCase(
    private val database: BetterMeDatabase,
    private val dataStoreManager: DataStoreManager,
    private val challengeRepository: ChallengeRepository,
    private val achievementRepository: AchievementRepository,
    private val groupTeamRepository: GroupTeamRepository
) {

    suspend operator fun invoke() {
        // Skip only when the device already has the *full current* catalog. Comparing
        // against an expected total catches both first-launch and the case where the
        // app upgrades and ships more challenges (the previous flag-only guard would
        // leave older installs forever stuck at the initial catalog size).
        // OnConflictStrategy.REPLACE on the DAO inserts means re-seeding is idempotent
        // for already-existing rows; only newly-added IDs actually mutate the table.
        val expectedChallenges = ChallengesSeed.challenges().size +
            UpcomingChallengesSeed.upcoming().size +
            EliteChallengesSeed.elite().size
        val expectedBadges = BadgesSeed.badges.size
        val flagSet = dataStoreManager.isChallengesSeeded()
        val challengeCount = challengeRepository.count()
        val badgeCount = achievementRepository.count()
        if (flagSet && challengeCount >= expectedChallenges && badgeCount >= expectedBadges) {
            return
        }

        database.withTransaction {
            achievementRepository.insertAll(BadgesSeed.badges)
            challengeRepository.insertAll(ChallengesSeed.challenges())
            challengeRepository.insertAll(UpcomingChallengesSeed.upcoming())
            // Elite tier (10 HARD + 10 LEGENDARY) ships through the same Challenge
            // pipeline as everything else — Discover, Detail, Join, check-ins all
            // work without further plumbing. A subset carry future start_dates so
            // they appear in the "Sắp diễn ra" carousel until activation.
            challengeRepository.insertAll(EliteChallengesSeed.elite())
            groupTeamRepository.insertAll(GroupTeamsSeed.teams)
        }

        dataStoreManager.setChallengesSeeded()
    }
}

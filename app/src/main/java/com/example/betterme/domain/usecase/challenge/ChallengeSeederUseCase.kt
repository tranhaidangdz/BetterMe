package com.example.betterme.domain.usecase.challenge

import androidx.room.withTransaction
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.database.BetterMeDatabase
import com.example.betterme.data.seed.BadgesSeed
import com.example.betterme.data.seed.ChallengesSeed
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
        // Skip when DataStore says we've already seeded AND the DB still has the rows.
        // The DB-count guard catches the case where Room destructive migration wiped the
        // tables but the DataStore flag survived.
        val flagSet = dataStoreManager.isChallengesSeeded()
        val hasChallenges = challengeRepository.count() > 0
        if (flagSet && hasChallenges) return

        database.withTransaction {
            achievementRepository.insertAll(BadgesSeed.badges)
            challengeRepository.insertAll(ChallengesSeed.challenges())
            challengeRepository.insertAll(UpcomingChallengesSeed.upcoming())
            groupTeamRepository.insertAll(GroupTeamsSeed.teams)
        }

        dataStoreManager.setChallengesSeeded()
    }
}

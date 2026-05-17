package com.example.betterme.domain.usecase.challenge

import androidx.room.withTransaction
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.database.BetterMeDatabase
import com.example.betterme.data.seed.BadgesSeed
import com.example.betterme.data.seed.ChallengesSeed
import com.example.betterme.data.seed.EliteChallengesSeed
import com.example.betterme.data.seed.GroupTeamsSeed
import com.example.betterme.data.seed.PrestigeChallengesSeed
import com.example.betterme.data.seed.StarterChallengesSeed
import com.example.betterme.data.seed.UpcomingChallengesSeed
import com.example.betterme.domain.repository.AchievementRepository
import com.example.betterme.domain.repository.ChallengeRepository
import com.example.betterme.domain.repository.GroupTeamRepository

/**
 * Seeds the challenge catalog, badge catalog, and mock group teams on first launch
 * (and after destructive Room migrations). Idempotent via [DataStoreManager.isChallengesSeeded].
 *
 * The challenge catalog is the union of five coexisting sources — they are merged,
 * never replaced:
 * - [ChallengesSeed]           — 48 entries, IDs 1..48 (mix of EASY/MEDIUM/HARD/LEGENDARY)
 * - [UpcomingChallengesSeed]   — 20 entries, IDs 100..119 (seasonal future-dated)
 * - [EliteChallengesSeed]      — 20 entries, IDs 200..219 (10 HARD + 10 LEGENDARY)
 * - [StarterChallengesSeed]    — 20 entries, IDs 300..319 (10 EASY + 10 MEDIUM)
 * - [PrestigeChallengesSeed]   — 15 entries, IDs 400..414 (5 MEDIUM + 5 HARD + 5 LEGENDARY)
 *
 * All five insert into the same `challenges` table via the same DAO, so every screen
 * (Discover, Overview, Detail) sees a single unified catalog of 123 challenges.
 *
 * Order matters: badges first (challenges FK to reward_badge_id), then challenges, then teams
 * (teams FK to challenge_id). DAO inserts use OnConflictStrategy.IGNORE so re-seeding on
 * upgrade adds new IDs without disturbing existing rows or cascade-deleting any user's
 * joined challenges and check-in logs.
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
        // OnConflictStrategy.IGNORE on the catalog DAO inserts means re-seeding is
        // idempotent for already-existing rows; only newly-added IDs actually mutate
        // the table. This is critical because UserChallengeEntity has CASCADE on its
        // FK to challenges — REPLACE would delete-then-reinsert and cascade-wipe
        // every joined challenge + log row a user has accumulated.
        val expectedChallenges = ChallengesSeed.challenges().size +
            UpcomingChallengesSeed.upcoming().size +
            EliteChallengesSeed.elite().size +
            StarterChallengesSeed.starter().size +
            PrestigeChallengesSeed.prestige().size
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
            // Starter tier (10 EASY + 10 MEDIUM) added to keep the catalog balanced
            // for new users — beginner-friendly entries with smaller commitments and
            // proportionally lower coin rewards.
            challengeRepository.insertAll(StarterChallengesSeed.starter())
            // Prestige expansion (5 MEDIUM + 5 HARD + 5 LEGENDARY) — hand-curated
            // themes (No Sugar 90d, Deep Work 100d, 75 Hard, Wake Before 6AM 60d,
            // Digital Detox Master) anchoring the long-form ladder. Same
            // OnConflictStrategy.IGNORE semantics: new IDs only.
            challengeRepository.insertAll(PrestigeChallengesSeed.prestige())
            groupTeamRepository.insertAll(GroupTeamsSeed.teams)
        }

        dataStoreManager.setChallengesSeeded()
    }
}

package com.example.betterme.data.leaderboard

import com.example.betterme.domain.leaderboard.GlobalLeaderboardEntry
import com.example.betterme.domain.leaderboard.GlobalScoreFormula
import kotlin.random.Random

/**
 * Deterministic seeded competitor pool for the GLOBAL leaderboard.
 * Mirrors [HybridCompetitorSeeder]'s strategy — pool keyed by
 * [seasonKey], stable across devices and across in-session reads.
 *
 * Why a separate seeder from the per-challenge one:
 *  - Different stat shape (totalCompletedHabits, longestStreak,
 *    completedChallenges instead of one-challenge stats).
 *  - Different distribution: global pool sits in a higher score band
 *    (~500-2500) to span Bronze through Diamond tiers and make tier
 *    promotion feel realistic.
 *  - Pool size is fixed at ~80 so the leaderboard always has enough
 *    rivals around any score band.
 *
 * The user lands in the merged pool sorted by score, so a new user
 * with no check-ins yet ranks at the bottom; a Silver-tier user lands
 * mid-pack; a Gold-tier user lands in the top 30; etc.
 */
class GlobalCompetitorSeeder {

    /**
     * Generate the deterministic seeded pool. Re-seeds the [Random]
     * each call so the result is reproducible per-(seasonKey).
     */
    fun seedFor(seasonKey: String, poolSize: Int = 80): List<GlobalLeaderboardEntry> {
        val seed = makeSeed(seasonKey)
        val rng = Random(seed)
        val now = System.currentTimeMillis()

        val names = NAME_POOL.shuffled(rng)
        val avatars = AVATAR_POOL.shuffled(rng)
        val target = minOf(poolSize, names.size)

        // Score curve: top ~10 sit in Diamond/Master band, middle in
        // Gold/Platinum, bottom in Bronze/Silver. The curve uses a
        // square-root drop-off so high ranks have meaningful spacing
        // and the bottom doesn't compress.
        return (0 until target).map { i ->
            val depth = i.toFloat() / target.toFloat()
            val baseScore = ((1f - depth) * 2500f + 250f + rng.nextInt(-60, 60)).toInt()
                .coerceAtLeast(120)

            // Decompose back into the formula's parts. Tilt the
            // decomposition so longestStreak carries some of the
            // visible weight — it's the field most users care about.
            val challenges = (depth.let { 1f - it } * 6f + rng.nextInt(0, 3)).toInt().coerceAtLeast(0)
            val streak = ((baseScore / 10f) * 0.35f).toInt().coerceIn(4, 60)
            val consistency = (60 - (depth * 60f)).toInt().coerceIn(0, 100)
            val coins = (baseScore - challenges * 50 - streak * 10 - consistency).coerceAtLeast(0) / 2
            val habits = ((baseScore - streak * 10 - coins - challenges * 50 - consistency) / 5).coerceAtLeast(0)
            val finalScore = GlobalScoreFormula.compute(
                totalCompletedHabits = habits,
                longestStreak = streak,
                earnedCoins = coins,
                completedChallenges = challenges,
                monthlyConsistencyBonus = consistency
            )

            GlobalLeaderboardEntry(
                userId = "global_rival_${seasonKey}_$i",
                displayName = names[i],
                avatarUrl = avatars[i % avatars.size],
                totalCompletedHabits = habits,
                longestStreak = streak,
                earnedCoins = coins,
                completedChallenges = challenges,
                monthlyConsistencyBonus = consistency,
                totalScore = finalScore,
                updatedAt = now - rng.nextLong(0, 5L * 24 * 60 * 60 * 1000),
                isSeededRival = true
            )
        }
    }

    fun makeSeed(seasonKey: String): Long =
        seasonKey.hashCode().toLong() xor SEED_SALT

    private companion object {
        const val SEED_SALT: Long = 0xBE_77E_67_3DL

        /** Same name pool as the per-challenge seeder — keeps the
         *  Vietnamese-heavy roster consistent across surfaces. */
        val NAME_POOL: List<String> = listOf(
            "Minh", "Linh", "Hà", "Anh", "Đăng", "Trang", "Nam", "Mai", "Quân", "Hùng",
            "Hương", "Tùng", "Phương", "Việt", "Hải", "Long", "Quỳnh", "Khôi", "An", "Bảo",
            "Châu", "Duy", "Giang", "Khánh", "Kiên", "Lan", "Loan", "Ngọc", "Như", "Phúc",
            "Quang", "Sơn", "Thảo", "Thắng", "Thu", "Thuỳ", "Tiến", "Trí", "Trinh", "Tuấn",
            "Tuyết", "Vy", "Yến", "Hà My", "Phương Anh", "Bích Ngọc", "Hoài Nam", "Đức Anh",
            "Khánh Linh", "Thanh Hằng", "Hồng Nhung", "Mạnh Cường", "Quốc Bảo", "Hữu Phước",
            "Thị Hà", "Việt Hà", "Đại Dương", "Bá Đạt",
            "Alex", "Sara", "Liam", "Mia", "Noah", "Emma", "Lucas", "Olivia", "Ethan",
            "Sophia", "Hiroshi", "Yuki", "Akira", "Mei", "Hana", "Tao", "Ming", "Hyun",
            "Jia", "Soo", "Daniel", "Hannah", "Adrian", "Chloe", "Felix", "Nora", "Leo",
            "Ivy", "Owen", "Maya", "Jonas", "Léa"
        )

        val AVATAR_POOL: List<String> = (1..40).map { seed ->
            "https://api.dicebear.com/9.x/personas/png?seed=bm-global-$seed&backgroundType=gradientLinear"
        }
    }
}

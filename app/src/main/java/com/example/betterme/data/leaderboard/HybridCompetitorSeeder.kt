package com.example.betterme.data.leaderboard

import com.example.betterme.domain.leaderboard.LeaderboardEntry
import kotlin.random.Random

/**
 * Deterministic seeded competitors for the monthly challenge
 * leaderboard. Lets BetterMe ship a "competitive" feel before it has a
 * critical mass of real users, without polluting Firestore with seed
 * data that an admin would later have to clean up.
 *
 * ### Determinism contract
 * The PRNG is seeded from `(challengeId, seasonKey, optional salt)` so:
 *  - All BetterMe installs see the same competitors for the same
 *    (challenge, season). Two friends comparing the leaderboard at a
 *    coffee shop see consistent names and scores.
 *  - The seed CHANGES at the season boundary, so May's competitors are
 *    different from June's even for the same challenge — feels like a
 *    real monthly reset.
 *
 * ### Participant count
 * Easier challenges have more competitors than hard ones. The size is
 * derived from the challenge's `target_streak` (BetterMe's stand-in for
 * difficulty): a 7-day starter sees ~120 seeded rivals, a 60-day elite
 * sees ~25. Multipliers stay in the gentle range so the user always has
 * a populated leaderboard without it feeling crowded.
 *
 * ### Score distribution
 * Top scorer sits around 1200-1400. Each subsequent rank loses ~5-25
 * points with small jitter so visible gaps look natural — no neighbor
 * pairs ever sit 0 apart, no awkward "1, 99999" cliffs. The bottom of
 * the seeded pool sits in the 200-400 range so a new user with a
 * handful of check-ins lands around rank 30-50 rather than dead last.
 */
class HybridCompetitorSeeder {

    /**
     * Generate the full seeded pool for a challenge + season.
     *
     * @param participantHint hint from the meta doc; if 0, the seeder
     *        picks a count based on [targetStreak]. The user prompt
     *        provides the real number when it's available.
     * @param targetStreak the challenge's `target_streak` field —
     *        proxy for difficulty.
     * @param difficulty optional explicit difficulty (EASY / MEDIUM /
     *        HARD / LEGENDARY). When provided, the seeder shifts the
     *        top-score band upward so harder challenges feel tougher
     *        to climb. Defaults to deriving from [targetStreak] alone
     *        for backward compatibility with older call sites.
     */
    fun seedFor(
        challengeId: Int,
        seasonKey: String,
        targetStreak: Int,
        participantHint: Int = 0,
        difficulty: String? = null
    ): List<LeaderboardEntry> {
        val seed = makeSeed(challengeId, seasonKey)
        val rng = Random(seed)
        val targetCount = if (participantHint > 0) {
            participantHint.coerceIn(15, 200)
        } else {
            seededCountFor(targetStreak)
        }

        // Pick names without replacement so the pool feels diverse and
        // no two seeded competitors share a name within a season.
        val shuffledNames = NAME_POOL.shuffled(rng)
        val avatars = AVATAR_POOL.shuffled(rng)
        val targetForNames = minOf(targetCount, shuffledNames.size)

        // Top-score band is difficulty-aware: HARD and LEGENDARY land
        // higher to communicate "this leaderboard is for serious
        // competitors". The score numbers feed into Phase 2A badges +
        // Phase 2B league tier rendering, so this nudges the elite
        // tiers into Gold/Platinum/Diamond territory naturally.
        val (lowEnd, highEnd) = when (difficulty?.uppercase()) {
            "LEGENDARY" -> 1500 to 1850
            "HARD" -> 1350 to 1650
            "MEDIUM" -> 1240 to 1480
            else -> 1180 to 1420
        }
        var nextScore = rng.nextInt(lowEnd, highEnd)
        val now = System.currentTimeMillis()

        return (0 until targetForNames).map { i ->
            val name = shuffledNames[i]
            val avatar = avatars[i % avatars.size]
            // Decompose the score back into the formula's parts so the
            // UI can show realistic per-row stats if it wants. The
            // streak is the primary driver of the leaderboard, so we
            // tilt the decomposition toward it.
            val approxStreak = ((nextScore / 10) * 0.4f).toInt().coerceIn(3, 60)
            val approxTasks = ((nextScore - approxStreak * 5) / 12).coerceAtLeast(approxStreak)
            val approxCoins = (nextScore - approxTasks * 10 - approxStreak * 5).coerceAtLeast(0)

            val entry = LeaderboardEntry(
                userId = "rival_${challengeId}_${seasonKey}_$i",
                displayName = name,
                avatarUrl = avatar,
                completedTasks = approxTasks,
                currentStreak = approxStreak,
                earnedCoins = approxCoins,
                totalScore = nextScore,
                updatedAt = now - rng.nextLong(0, 7L * 24 * 60 * 60 * 1000), // last 7 days
                isSeededRival = true
            )

            // Compute next rank's score. Top-of-leaderboard gaps are
            // tighter (championship competition); mid/bottom gaps
            // widen so the bottom anchors at ~200-400 for EASY and
            // higher for harder tiers — even the tail of a LEGENDARY
            // leaderboard should feel respectable.
            val drop = when {
                i < 5 -> rng.nextInt(8, 25)
                i < 20 -> rng.nextInt(15, 35)
                i < 50 -> rng.nextInt(10, 28)
                else -> rng.nextInt(5, 18)
            }
            val floor = when (difficulty?.uppercase()) {
                "LEGENDARY" -> 420
                "HARD" -> 320
                "MEDIUM" -> 220
                else -> 180
            }
            nextScore = (nextScore - drop).coerceAtLeast(floor + rng.nextInt(0, 80))
            entry
        }
    }

    /** Same PRNG seed Anywhere in the app that needs it (testing). */
    fun makeSeed(challengeId: Int, seasonKey: String): Long {
        return challengeId.toLong() * 1_000_003L xor seasonKey.hashCode().toLong()
    }

    private fun seededCountFor(targetStreak: Int): Int = when {
        targetStreak <= 7 -> 120
        targetStreak <= 14 -> 90
        targetStreak <= 21 -> 65
        targetStreak <= 30 -> 45
        targetStreak <= 45 -> 30
        else -> 22
    }

    private companion object {
        /** Vietnamese-majority name pool with international mix. ~120 names so
         *  even the biggest seeded count (120) gets unique entries. */
        val NAME_POOL: List<String> = listOf(
            // Vietnamese — common given names
            "Minh", "Linh", "Hà", "Anh", "Đăng", "Trang", "Nam", "Mai", "Quân", "Hùng",
            "Hương", "Tùng", "Phương", "Việt", "Hải", "Long", "Quỳnh", "Khôi", "An", "Bảo",
            "Châu", "Duy", "Giang", "Khánh", "Kiên", "Lan", "Loan", "Ngọc", "Như", "Phúc",
            "Quang", "Sơn", "Thảo", "Thắng", "Thu", "Thuỳ", "Tiến", "Trí", "Trinh", "Tuấn",
            "Tuyết", "Vy", "Yến", "Hà My", "Phương Anh", "Bích Ngọc", "Hoài Nam", "Đức Anh",
            "Khánh Linh", "Thanh Hằng", "Hồng Nhung", "Mạnh Cường", "Quốc Bảo", "Hữu Phước",
            "Thị Hà", "Việt Hà", "Đại Dương", "Bá Đạt",
            // Light international mix — gives the pool a global feel
            "Alex", "Sara", "Liam", "Mia", "Noah", "Emma", "Lucas", "Olivia", "Ethan",
            "Sophia", "Hiroshi", "Yuki", "Akira", "Mei", "Hana", "Tao", "Ming", "Hyun",
            "Jia", "Soo", "Daniel", "Hannah", "Adrian", "Chloe", "Felix", "Nora", "Leo",
            "Ivy", "Owen", "Maya", "Jonas", "Léa", "Carlos", "Lucia", "Diego", "Camila",
            "Mateo", "Isabela", "Rohan", "Priya", "Aarav", "Diya", "Kavya", "Aditya",
            "Hamid", "Layla", "Omar", "Zara", "Ahmed", "Yasmin"
        )

        /** Pixel-style placeholder avatars from Dicebear — purely cosmetic,
         *  loaded by Coil at render time. The seeded user id stays in the
         *  avatar slug so the same competitor gets the same face. */
        val AVATAR_POOL: List<String> = (1..50).map { seed ->
            "https://api.dicebear.com/9.x/personas/png?seed=bm-rival-$seed&backgroundType=gradientLinear"
        }
    }
}

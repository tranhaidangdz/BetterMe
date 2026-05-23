package com.example.betterme.domain.usecase.challenge

import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.challenge.UserChallengeStatus
import com.example.betterme.domain.repository.ChallengeLogRepository
import com.example.betterme.domain.repository.UserChallengeRepository
import com.example.betterme.utils.DateUtils
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds the rich text payload sent to the native Android share sheet from the
 * Challenge Overview screen.
 *
 * Sections, in order:
 *  1. Title + overall stats (joined / completed / completion rate).
 *  2. Active challenges with title, day count, % progress, and last check-in date.
 *  3. Ended challenges (COMPLETED / FAILED / ABANDONED) with finish status and date.
 *  4. Recent check-in dates across all challenges, max 7 most recent.
 *
 * The output stays under typical SMS / DM length budgets (~1500 chars in the worst
 * case with 10+ active challenges). All Vietnamese strings match the in-app copy so
 * the share reads naturally in Messenger / Zalo / Facebook.
 */
class BuildChallengeProgressShareTextUseCase(
    private val dataStoreManager: DataStoreManager,
    private val userChallengeRepository: UserChallengeRepository,
    private val challengeLogRepository: ChallengeLogRepository
) {

    suspend operator fun invoke(): String {
        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        if (userId.isBlank()) {
            return DEFAULT_FALLBACK
        }

        val joined = userChallengeRepository.observeWithDetails(userId).first()
        if (joined.isEmpty()) {
            return DEFAULT_FALLBACK
        }

        val active = joined.filter { it.userChallenge.status == UserChallengeStatus.ACTIVE }
        val completedSuccess = joined.count {
            it.userChallenge.status == UserChallengeStatus.COMPLETED
        }
        val terminal = joined.filter { UserChallengeStatus.isTerminal(it.userChallenge.status) }
        val rate = if (joined.isNotEmpty()) (completedSuccess * 100) / joined.size else 0

        // Collect distinct recent DONE check-in dates across the user's joined rows so
        // the share advertises consistency, not just totals.
        val recentCheckIns: List<Long> = joined
            .flatMap { details ->
                challengeLogRepository.getDoneDates(details.userChallenge.id)
            }
            .map { DateUtils.startOfDay(it) }
            .distinct()
            .sortedDescending()
            .take(7)

        return buildString {
            append("🎯 Tiến độ thử thách của tôi trên BetterMe\n\n")

            append("📊 Tổng quan:\n")
            append("• Đang tham gia: ${active.size} thử thách\n")
            append("• Đã hoàn thành: $completedSuccess\n")
            append("• Tỉ lệ thành công: $rate%\n")

            if (active.isNotEmpty()) {
                append("\n🔥 Đang diễn ra:\n")
                active.take(MAX_ACTIVE_LISTED).forEach { details ->
                    val uc = details.userChallenge
                    val c = details.challenge
                    val target = c.target_streak.coerceAtLeast(1)
                    append("• ${c.title}: ${uc.current_streak}/$target ngày — ${uc.progress_pct}%")
                    uc.last_check_in_date?.let {
                        append(" (check-in ${shortDate(it)})")
                    }
                    append("\n")
                }
                if (active.size > MAX_ACTIVE_LISTED) {
                    append("• …và ${active.size - MAX_ACTIVE_LISTED} thử thách khác\n")
                }
            }

            if (terminal.isNotEmpty()) {
                append("\n🏆 Đã kết thúc:\n")
                terminal.take(MAX_TERMINAL_LISTED).forEach { details ->
                    val uc = details.userChallenge
                    val c = details.challenge
                    val statusLabel = when (uc.status) {
                        UserChallengeStatus.COMPLETED -> "Hoàn thành"
                        UserChallengeStatus.FAILED -> "Thất bại"
                        UserChallengeStatus.ABANDONED -> "Đã bỏ"
                        else -> "Đã kết thúc"
                    }
                    val date = uc.end_date?.let { shortDate(it) } ?: "—"
                    append("• ${c.title} — $statusLabel ($date)\n")
                }
                if (terminal.size > MAX_TERMINAL_LISTED) {
                    append("• …và ${terminal.size - MAX_TERMINAL_LISTED} thử thách khác\n")
                }
            }

            if (recentCheckIns.isNotEmpty()) {
                append("\n📅 Check-in gần đây:\n")
                append(recentCheckIns.joinToString(separator = ", ") { shortDate(it) })
                append("\n")
            }

            append("\nCùng mình xây thói quen tốt trên BetterMe nhé! 💪")
        }
    }

    private fun shortDate(ms: Long): String = dateFormatter.format(Date(ms))

    private companion object {
        const val MAX_ACTIVE_LISTED = 6
        const val MAX_TERMINAL_LISTED = 5
        const val DEFAULT_FALLBACK = "Mình đang xây thói quen tốt trên BetterMe — cùng tham gia nhé!"
        val dateFormatter = SimpleDateFormat("dd/MM", Locale.forLanguageTag("vi"))
    }
}

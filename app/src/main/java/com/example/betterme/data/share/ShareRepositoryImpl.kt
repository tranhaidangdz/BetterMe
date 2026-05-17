package com.example.betterme.data.share

import android.util.Log
import com.example.betterme.BuildConfig
import com.example.betterme.data.share.dto.CheckInDto
import com.example.betterme.data.share.dto.CreateShareRequest
import com.example.betterme.data.share.dto.ProfileDto
import com.example.betterme.data.share.dto.SnapshotDto
import com.example.betterme.data.share.dto.SummaryDto
import com.example.betterme.domain.repository.ShareRepository
import com.example.betterme.domain.share.ShareLink
import com.example.betterme.domain.share.ShareType
import com.example.betterme.domain.share.VerificationStatus
import com.example.betterme.domain.share.VerifiedCheckIn
import com.example.betterme.domain.share.VerifiedProfile
import com.example.betterme.domain.share.VerifiedShare
import com.example.betterme.domain.share.VerifiedSummary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Verifies-or-fails-loud: every read goes through the server, and
 * every parse failure / network blip surfaces as a typed
 * [VerificationStatus] so the UI never paints a partial snapshot.
 *
 * Local Room data is NEVER used to hydrate the viewer — the whole
 * point of the feature is that the displayed numbers are
 * server-attested.
 */
class ShareRepositoryImpl(
    private val api: ShareApi,
    private val auth: FirebaseAuth
) : ShareRepository {

    override suspend fun createShare(
        type: ShareType,
        displayName: String,
        avatarUrl: String?,
        currentStreakDays: Int,
        longestStreakDays: Int,
        completedChallenges: Int,
        legendaryChallenges: Int,
        checkIns: List<ShareRepository.CheckInInput>
    ): ShareLink {
        val token = currentIdToken()
            ?: throw IllegalStateException("Vui lòng đăng nhập để tạo link chia sẻ.")
        if (checkIns.isEmpty()) {
            throw IllegalStateException("Chưa có check-in nào để chia sẻ.")
        }

        val request = CreateShareRequest(
            type = type.wireValue,
            profile = ProfileDto(displayName = displayName, avatarUrl = avatarUrl),
            summary = SummaryDto(
                currentStreakDays = currentStreakDays,
                longestStreakDays = longestStreakDays,
                completedChallenges = completedChallenges,
                legendaryChallenges = legendaryChallenges
            ),
            checkIns = checkIns.map {
                CheckInDto(
                    itemId = it.itemId,
                    itemTitle = it.itemTitle,
                    timestamp = it.timestamp,
                    kind = it.kind.name,
                    note = it.note
                )
            }
        )

        val response = api.createShare("Bearer $token", request)
        val webLink = buildWebLink(response.shareId)
        val richMessage = buildRichMessage(
            displayName = displayName,
            totalCheckIns = checkIns.size,
            streakDays = currentStreakDays,
            legendaryChallenges = legendaryChallenges,
            link = webLink
        )
        return ShareLink(
            shareId = response.shareId,
            deepLink = response.deepLink,
            webLink = webLink,
            richMessage = richMessage,
            createdAt = response.createdAt
        )
    }

    override suspend fun loadShare(shareId: String): ShareRepository.LoadResult {
        return try {
            val response = api.getShare(shareId)
            val status = when (response.status.uppercase()) {
                "VALID" -> VerificationStatus.VALID
                "INVALID" -> VerificationStatus.INVALID
                "NOT_FOUND" -> VerificationStatus.NOT_FOUND
                else -> VerificationStatus.INVALID
            }
            if (status != VerificationStatus.VALID || response.snapshot == null) {
                ShareRepository.LoadResult(status, null)
            } else {
                ShareRepository.LoadResult(
                    status = VerificationStatus.VALID,
                    share = response.snapshot.toDomain()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "loadShare failed for $shareId", e)
            ShareRepository.LoadResult(VerificationStatus.NETWORK, null)
        }
    }

    override suspend fun verifyShare(shareId: String): VerificationStatus = try {
        when (api.verifyShare(shareId).status.uppercase()) {
            "VALID" -> VerificationStatus.VALID
            "INVALID" -> VerificationStatus.INVALID
            "NOT_FOUND" -> VerificationStatus.NOT_FOUND
            else -> VerificationStatus.INVALID
        }
    } catch (e: Exception) {
        Log.w(TAG, "verifyShare failed for $shareId", e)
        VerificationStatus.NETWORK
    }

    // ─── Helpers ───────────────────────────────────────────────────

    private suspend fun currentIdToken(): String? {
        val user = auth.currentUser ?: return null
        return try {
            user.getIdToken(false).await().token
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch Firebase ID token", e)
            null
        }
    }

    /**
     * Build the user-facing HTTPS link. Today it points directly at
     * the Cloud Functions URL with `?id=` query param — same URL the
     * function serves browser HTML at. If Firebase Hosting rewrites
     * are configured (`/share/<id>` → getShare), swap this method's
     * base prefix to that pretty URL; nothing else changes.
     */
    private fun buildWebLink(shareId: String): String {
        val base = BuildConfig.SHARE_FUNCTIONS_BASE_URL.removeSuffix("/")
        return "$base/getShare?id=$shareId"
    }

    /** Vietnamese rich-text the user posts to Messenger / Zalo / FB. */
    private fun buildRichMessage(
        displayName: String,
        totalCheckIns: Int,
        streakDays: Int,
        legendaryChallenges: Int,
        link: String
    ): String = buildString {
        append("🔥 ").append(displayName).append(" đã hoàn thành ")
            .append(totalCheckIns).append(" check-in trên BetterMe!\n")
        if (streakDays > 0) {
            append("💪 Đang giữ chuỗi ").append(streakDays).append(" ngày liên tiếp\n")
        }
        if (legendaryChallenges > 0) {
            append("🏆 ").append(legendaryChallenges)
                .append(" thử thách huyền thoại đã hoàn thành\n")
        }
        append("\n✔ Xác minh tại: ").append(link)
    }

    // ─── Mappers ───────────────────────────────────────────────────

    private fun SnapshotDto.toDomain(): VerifiedShare = VerifiedShare(
        shareId = shareId,
        userId = userId,
        type = ShareType.fromWire(type) ?: ShareType.FULL_HISTORY,
        profile = VerifiedProfile(
            displayName = profile.displayName,
            avatarUrl = profile.avatarUrl
        ),
        summary = VerifiedSummary(
            totalCheckIns = summary.totalCheckIns,
            uniqueItems = summary.uniqueItems,
            earliestTimestamp = summary.earliestTimestamp,
            latestTimestamp = summary.latestTimestamp,
            currentStreakDays = summary.currentStreakDays,
            longestStreakDays = summary.longestStreakDays,
            completedChallenges = summary.completedChallenges,
            legendaryChallenges = summary.legendaryChallenges
        ),
        checkIns = checkIns.mapNotNull { it.toDomain() },
        createdAt = createdAt,
        expiresAt = expiresAt
    )

    private fun CheckInDto.toDomain(): VerifiedCheckIn? {
        if (itemId.isBlank() || itemTitle.isBlank() || timestamp <= 0L) return null
        val kindEnum = when (kind.uppercase()) {
            "CHALLENGE" -> VerifiedCheckIn.Kind.CHALLENGE
            else -> VerifiedCheckIn.Kind.HABIT
        }
        return VerifiedCheckIn(
            itemId = itemId,
            itemTitle = itemTitle,
            timestamp = timestamp,
            kind = kindEnum,
            note = note,
            proofHash = proofHash.orEmpty()
        )
    }

    private companion object {
        const val TAG = "ShareRepo"
    }
}

package com.example.betterme.presentation.challenge.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.R
import com.example.betterme.presentation.challenge.badges.components.BadgeSection
import com.example.betterme.presentation.challenge.badges.components.BadgeStatusFilterRow
import com.example.betterme.presentation.challenge.badges.components.BadgeSummaryCard
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChallengeBadgesScreen(
    onBackClick: () -> Unit,
    viewModel: ChallengeBadgesViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()

    val filteredSections = state.sections
        .map { section ->
            val filtered = when (state.filter) {
                BadgeStatusFilter.All -> section.badges
                BadgeStatusFilter.Earned -> section.badges.filter { it.isEarned }
                BadgeStatusFilter.Locked -> section.badges.filter { !it.isEarned }
            }
            section.copy(badges = filtered)
        }
        .filter { it.badges.isNotEmpty() }

    val lockedCount = state.totalBadges - state.earnedBadges

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundSecondary)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                BetterMeTopBar(
                    leadingIconRes = R.drawable.ic_arrow_left,
                    title = "Hệ thống huy hiệu",
                    onLeadingClick = onBackClick
                )
            }
            // Summary card and filter row are stable chrome.  Render them even
            // during loading so the screen has visible structure instead of a
            // single centered spinner on a blank canvas.
            item {
                BadgeSummaryCard(
                    earned = state.earnedBadges,
                    total = state.totalBadges
                )
            }
            item {
                BadgeStatusFilterRow(
                    selected = state.filter,
                    earnedCount = state.earnedBadges,
                    lockedCount = lockedCount,
                    onSelect = { viewModel.processIntent(ChallengeBadgesIntent.SetFilter(it)) }
                )
            }

            // Three mutually-exclusive body states:
            //  - loading        → spinner inline so the user knows data is coming
            //  - filtered empty → friendly empty-state message keyed to the active filter
            //  - has sections   → the regular grid
            when {
                state.isLoading && state.sections.isEmpty() -> item {
                    LoadingCell()
                }
                filteredSections.isEmpty() -> item {
                    EmptyStateCell(
                        filter = state.filter,
                        totalBadges = state.totalBadges,
                        earnedBadges = state.earnedBadges
                    )
                }
                else -> items(filteredSections, key = { it.title }) { section ->
                    BadgeSection(section = section)
                }
            }
        }
    }
}

/**
 * Centered spinner used while the badge catalog + earned-set are being loaded.
 * The card chrome above (summary + filter row) stays visible so the screen
 * doesn't briefly look broken between the initial-state and first emission.
 */
@Composable
private fun LoadingCell() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = BetterMeColors.Primary.Primary
        )
    }
}

/**
 * Empty-state card shown when the active filter produces zero sections — e.g.
 * "Đã đạt" with no badges yet, or "Chưa đạt" when the user has completed the
 * full collection.  The message is keyed to the filter so the user knows what
 * action (if any) would change the state.
 */
@Composable
private fun EmptyStateCell(
    filter: BadgeStatusFilter,
    totalBadges: Int,
    earnedBadges: Int
) {
    val (emoji, title, hint) = when {
        // Pure "no catalog at all" — usually means seed data hasn't run yet
        // (cold install, blocked seed transaction).  Tell the user to retry
        // after a sync rather than leaving them on a blank screen.
        totalBadges == 0 -> Triple(
            "🏅",
            "Chưa có huy hiệu nào",
            "Hệ thống huy hiệu chưa được khởi tạo. Hãy thử mở lại ứng dụng sau khi đồng bộ dữ liệu."
        )
        filter == BadgeStatusFilter.Earned -> Triple(
            "✨",
            "Bạn chưa đạt được huy hiệu nào",
            "Hoàn thành thử thách đầu tiên của bạn để mở khóa huy hiệu — bộ sưu tập đang chờ bạn ở đây!"
        )
        filter == BadgeStatusFilter.Locked && earnedBadges == totalBadges -> Triple(
            "🎉",
            "Tuyệt vời!",
            "Bạn đã thu thập đủ toàn bộ huy hiệu của BetterMe. Hãy quay lại khi có huy hiệu mới được thêm vào!"
        )
        else -> Triple(
            "🔍",
            "Không có huy hiệu nào trong bộ lọc này",
            "Hãy chọn bộ lọc khác để xem thêm huy hiệu."
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 56.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = BetterMeTypography.Title.Medium.Bold,
            color = BetterMeColors.Text.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = hint,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}

package com.example.betterme.presentation.share.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.betterme.R
import com.example.betterme.domain.share.VerificationStatus
import com.example.betterme.domain.share.VerifiedShare
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.share.components.ShareCheckInRow
import com.example.betterme.presentation.share.components.ShareSummaryGrid
import com.example.betterme.presentation.share.components.VerifiedFirebaseBadge
import com.example.betterme.presentation.share.components.VerifiedFirebaseBadgeCompact
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography
import org.koin.androidx.compose.koinViewModel

/**
 * Public profile surface — entered via `betterme://profile/{userId}`
 * or in-app navigation. Renders the same Firestore snapshot the share
 * viewer uses but with a Strava/Duolingo-style profile layout:
 *
 *   1. Hero card        — large avatar, display name, compact
 *                          verified chip beside the name.
 *   2. Verified badge   — full-width "Đã xác minh bởi Firebase" pill.
 *   3. Stat grid        — 4 cells (Check-in / Streak / Best / Trophies).
 *   4. Recent timeline  — last 30 check-ins as a peek list.
 *
 * Reuses [VerifiedFirebaseBadge], [ShareSummaryGrid], [ShareCheckInRow]
 * so the visual language matches the share viewer exactly.
 */
@Composable
fun PublicProfileScreen(
    userId: String,
    onBackClick: () -> Unit,
    viewModel: PublicProfileViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.processIntent(PublicProfileIntent.Load(userId))
    }

    Scaffold(
        containerColor = BetterMeColors.BackGround.BackgroundSecondary
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .statusBarsPadding()
        ) {
            BetterMeTopBar(
                leadingIconRes = R.drawable.ic_arrow_left,
                title = "Hồ sơ công khai",
                onLeadingClick = onBackClick
            )
            when (val ui = state.ui) {
                PublicProfileUi.Loading -> LoadingState()
                is PublicProfileUi.Loaded -> ProfileBody(share = ui.share)
                is PublicProfileUi.Missing -> MissingState(reason = ui.reason)
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = BetterMeColors.Primary.Primary, strokeWidth = 2.4.dp)
    }
}

@Composable
private fun MissingState(reason: VerificationStatus) {
    val (emoji, primary, secondary) = when (reason) {
        VerificationStatus.NOT_FOUND -> Triple(
            "🔎",
            "Người này chưa chia sẻ tiến độ.",
            "Yêu cầu họ mở BetterMe và bấm \"Chia sẻ tiến độ\" để xuất bản hồ sơ."
        )
        VerificationStatus.NETWORK -> Triple(
            "📡",
            "Không kết nối được với Firebase.",
            "Hãy kiểm tra mạng và thử lại."
        )
        VerificationStatus.VALID -> Triple("✔", "OK", "")
    }
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = emoji, fontSize = 40.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = primary,
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = secondary,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
    }
}

@Composable
private fun ProfileBody(share: VerifiedShare) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeroCard(share = share) }
        item { VerifiedFirebaseBadge() }
        item { ShareSummaryGrid(share = share) }
        item {
            Text(
                text = "Hoạt động gần đây",
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        // Cap the timeline at 30 rows so the profile feels like a
        // glanceable surface — the full timeline lives on the share
        // viewer, accessible via the share link.
        items(share.checkIns.take(30), key = { "${it.itemId}-${it.date}" }) { row ->
            ShareCheckInRow(row = row)
        }
        if (share.checkIns.size > 30) {
            item {
                Text(
                    text = "… và ${share.checkIns.size - 30} check-in khác",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroCard(share: VerifiedShare) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = BetterMeTokens.CardElevation.Body,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero),
                ambientColor = BetterMeTokens.NeutralShadow.Ambient,
                spotColor = BetterMeTokens.NeutralShadow.Spot
            )
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Hero))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White, BetterMeColors.Primary.Primary.copy(alpha = 0.06f))
                )
            )
            .border(
                width = 1.dp,
                color = BetterMeColors.Primary.Primary.copy(alpha = BetterMeTokens.AccentAlpha.Soft),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Hero)
            )
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(BetterMeColors.Primary.Primary.copy(alpha = 0.12f))
                    .border(
                        width = 2.dp,
                        color = BetterMeColors.Primary.Primary.copy(alpha = 0.30f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (share.profile.avatarUrl != null) {
                    AsyncImage(
                        model = share.profile.avatarUrl,
                        contentDescription = share.profile.displayName,
                        modifier = Modifier.size(96.dp).clip(CircleShape)
                    )
                } else {
                    Text(
                        text = share.profile.displayName.firstOrNull()
                            ?.uppercaseChar()?.toString() ?: "👤",
                        style = BetterMeTypography.Title.Medium.Bold,
                        color = BetterMeColors.Primary.Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = share.profile.displayName,
                style = BetterMeTypography.Title.Medium.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            VerifiedFirebaseBadgeCompact()
        }
    }
}

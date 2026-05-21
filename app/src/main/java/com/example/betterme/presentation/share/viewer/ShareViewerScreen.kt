package com.example.betterme.presentation.share.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Public viewer that opens on `betterme://share/{userId}` deep links
 * or in-app preview. Renders only data returned by the Firestore
 * snapshot at `/shared_progress/{userId}` — never falls back to local
 * Room.
 *
 * Visual hierarchy (top → bottom):
 *  1. [VerifiedFirebaseBadge] — Strava/Duolingo-style verified pill.
 *  2. Profile card — avatar + display name + publish timestamp.
 *  3. [ShareSummaryGrid] — 4-stat row.
 *  4. Check-in timeline.
 *
 * Reusable components live in `presentation/share/components/` so the
 * Public Profile screen shares the same building blocks.
 */
@Composable
fun ShareViewerScreen(
    userId: String,
    onBackClick: () -> Unit,
    viewModel: ShareViewerViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(userId) {
        viewModel.processIntent(ShareViewerIntent.Load(userId))
    }

    LaunchedEffect(viewModel) {
        viewModel.singleEvent.collectLatest { event ->
            when (event) {
                is ShareViewerEvent.ShowMessage ->
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT)
                        .show()
            }
        }
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
                title = "Tiến độ đã xác minh",
                onLeadingClick = onBackClick
            )
            when (val ui = state.ui) {
                ShareViewerUi.Loading -> LoadingState()
                is ShareViewerUi.Verified -> VerifiedBody(share = ui.share)
                is ShareViewerUi.Invalid -> InvalidState(reason = ui.reason)
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
private fun InvalidState(reason: VerificationStatus) {
    val (emoji, primary, secondary) = when (reason) {
        VerificationStatus.NOT_FOUND -> Triple(
            "🔎",
            "Người này chưa chia sẻ tiến độ.",
            "Yêu cầu họ mở BetterMe và bấm \"Chia sẻ tiến độ\" để xuất bản dữ liệu."
        )
        VerificationStatus.NETWORK -> Triple(
            "📡",
            "Không kết nối được với máy chủ.",
            "Hãy kiểm tra mạng và thử mở lại link."
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
private fun VerifiedBody(share: VerifiedShare) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { VerifiedFirebaseBadge() }
        item { ProfileCard(share = share) }
        item { ShareSummaryGrid(share = share) }
        item {
            Text(
                text = "Lịch sử check-in (${share.checkIns.size})",
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        items(share.checkIns, key = { "${it.itemId}-${it.date}" }) { row ->
            ShareCheckInRow(row = row)
        }
    }
}

@Composable
private fun ProfileCard(share: VerifiedShare) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = BetterMeColors.Border.BorderLight,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(BetterMeColors.Gray.Gray3),
            contentAlignment = Alignment.Center
        ) {
            share.profile.avatarUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = share.profile.displayName,
                    modifier = Modifier.size(54.dp).clip(CircleShape)
                )
            } ?: Text(text = "👤", fontSize = 28.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = share.profile.displayName,
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Xuất bản: ${formatDate(share.publishedAt)}",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
    }
}

private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi"))
private fun formatDate(ms: Long): String = dateFmt.format(Date(ms))

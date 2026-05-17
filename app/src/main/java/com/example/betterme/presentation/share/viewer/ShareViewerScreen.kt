package com.example.betterme.presentation.share.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.betterme.domain.share.VerifiedCheckIn
import com.example.betterme.domain.share.VerifiedShare
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Server-verified share viewer.
 *
 *  - Renders ONLY data returned by `getShare` (HMAC re-validated
 *    server-side).
 *  - Headline strip: ✔ "Đã xác nhận bởi máy chủ BetterMe".
 *  - Stat grid + timeline of check-ins, each carrying a 6-char
 *    truncated proofHash for the user to eyeball.
 *  - Invalid / not-found / network states each get their own
 *    distinct surface so the viewer never quietly fakes data.
 */
@Composable
fun ShareViewerScreen(
    shareId: String,
    onBackClick: () -> Unit,
    viewModel: ShareViewerViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(shareId) {
        viewModel.processIntent(ShareViewerIntent.Load(shareId))
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
                is ShareViewerUi.Verified -> VerifiedBody(
                    share = ui.share,
                    isReverifying = state.isReverifying,
                    onReverify = { viewModel.processIntent(ShareViewerIntent.ReVerify) }
                )
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
        VerificationStatus.INVALID -> Triple(
            "❌",
            "Dữ liệu đã bị thay đổi.",
            "Snapshot này không còn khớp với chữ ký máy chủ — không đáng tin."
        )
        VerificationStatus.NOT_FOUND -> Triple(
            "🔎",
            "Link không tồn tại hoặc đã hết hạn.",
            "Snapshot chia sẻ có hiệu lực trong 90 ngày kể từ khi tạo."
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
private fun VerifiedBody(
    share: VerifiedShare,
    isReverifying: Boolean,
    onReverify: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { VerifiedBadge(onReverify = onReverify, isReverifying = isReverifying) }
        item { ProfileCard(share = share) }
        item { SummaryGrid(share = share) }
        item {
            Text(
                text = "Lịch sử check-in (${share.checkIns.size})",
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        items(share.checkIns, key = { "${it.itemId}-${it.timestamp}" }) { row ->
            CheckInRow(row = row)
        }
    }
}

@Composable
private fun VerifiedBadge(onReverify: () -> Unit, isReverifying: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(BetterMeColors.Green.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = BetterMeColors.Green.copy(alpha = 0.30f),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Pill)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "✔  Đã xác nhận bởi máy chủ BetterMe",
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Green,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                .background(BetterMeColors.Green.copy(alpha = 0.18f))
                .clickable(enabled = !isReverifying) { onReverify() }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isReverifying) "Đang kiểm tra…" else "Xác minh lại",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Green,
                fontWeight = FontWeight.SemiBold
            )
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
                text = "Snapshot: ${formatDate(share.createdAt)}",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
    }
}

@Composable
private fun SummaryGrid(share: VerifiedShare) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCell(
            label = "Check-in",
            value = "${share.summary.totalCheckIns}",
            modifier = Modifier.weight(1f)
        )
        StatCell(
            label = "Chuỗi hiện tại",
            value = "${share.summary.currentStreakDays}🔥",
            modifier = Modifier.weight(1f)
        )
        StatCell(
            label = "Thử thách",
            value = "${share.summary.completedChallenges}🏆",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = BetterMeColors.Border.BorderLight,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body)
            )
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = BetterMeTypography.Title.Small.Bold,
            color = BetterMeColors.Text.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary
        )
    }
}

@Composable
private fun CheckInRow(row: VerifiedCheckIn) {
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
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = if (row.kind == VerifiedCheckIn.Kind.CHALLENGE) "🏆" else "✅",
            fontSize = 20.sp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.itemTitle,
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = formatDate(row.timestamp),
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
            if (row.proofHash.isNotBlank()) {
                Text(
                    text = "🔒 " + row.proofHash.take(8) + "…",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
            }
        }
    }
}

private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("vi"))
private fun formatDate(ms: Long): String = dateFmt.format(Date(ms))

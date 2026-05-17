package com.example.betterme.presentation.share.sheet

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.betterme.domain.share.VerifiedShare
import com.example.betterme.presentation.share.utils.ShareCardRenderer
import com.example.betterme.presentation.share.utils.ShareIntentHelper
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

/**
 * Bottom sheet entry point for the simple verified share flow.
 *
 * Layout collapses to three states:
 *   1. Publishing — spinner + "Đang xuất bản tiến độ lên máy chủ..."
 *   2. Ready      — deep link visible + "Chia sẻ lại" button
 *   3. Error      — message + retry
 *
 * Auto-fires Publish on first composition so the user effectively
 * sees the spinner → ACTION_SEND chooser hand-off in one motion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareProgressBottomSheet(
    onDismiss: () -> Unit,
    viewModel: ShareProgressViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.singleEvent.collectLatest { event ->
            when (event) {
                is ShareProgressEvent.LaunchShareSheet -> {
                    val launched = ShareIntentHelper.shareText(
                        context = context,
                        subject = event.subject,
                        text = event.text
                    )
                    if (!launched) {
                        showToast(context, "Không tìm thấy ứng dụng chia sẻ phù hợp.")
                    }
                }
                is ShareProgressEvent.ShowMessage ->
                    showToast(context, event.message)
            }
        }
    }

    // Auto-publish on first sheet open. Subsequent re-opens after the
    // user dismissed publish again — they probably want a fresh
    // snapshot reflecting any new check-ins.
    LaunchedEffect(Unit) {
        viewModel.processIntent(ShareProgressIntent.Publish)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Header(onClose = onDismiss)
            Spacer(Modifier.height(14.dp))
            when (val ui = state.ui) {
                ShareProgressUi.Idle, ShareProgressUi.Publishing -> PublishingBody()
                is ShareProgressUi.Ready -> ReadyBody(
                    deepLink = ui.link.deepLink,
                    snapshot = ui.share,
                    onShareAgain = {
                        ShareIntentHelper.shareText(
                            context = context,
                            subject = "Tiến độ BetterMe của tôi",
                            text = ui.link.richMessage
                        )
                    },
                    onShareImage = {
                        val snapshot = ui.share ?: return@ReadyBody
                        val uri = ShareCardRenderer(context).renderToUri(snapshot)
                        ShareIntentHelper.shareImage(
                            context = context,
                            imageUri = uri,
                            subject = "Tiến độ BetterMe của tôi",
                            caption = ui.link.richMessage
                        )
                    }
                )
                is ShareProgressUi.Error -> ErrorBody(
                    message = ui.message,
                    onRetry = { viewModel.processIntent(ShareProgressIntent.Publish) }
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Header(onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BetterMeColors.Primary.Primary.copy(alpha = BetterMeTokens.AccentAlpha.Soft)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🔗", fontSize = 20.sp)
        }
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Chia sẻ tiến độ",
                style = BetterMeTypography.Title.Medium.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Người nhận sẽ thấy dữ liệu trực tiếp từ Firebase",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BetterMeColors.Gray.Gray3)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✕", color = BetterMeColors.Text.TextTertiary)
        }
    }
}

@Composable
private fun PublishingBody() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = BetterMeColors.Primary.Primary,
            strokeWidth = 2.6.dp
        )
        Text(
            text = "Đang xuất bản tiến độ lên Firebase…",
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextSecondary
        )
    }
}

@Composable
private fun ReadyBody(
    deepLink: String,
    snapshot: VerifiedShare?,
    onShareAgain: () -> Unit,
    onShareImage: () -> Unit
) {
    Column {
        Text(
            text = "✅  Đã xuất bản — link sẵn sàng để chia sẻ.",
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Green,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))
        LinkBlock(label = "Link chia sẻ", url = deepLink)
        Spacer(Modifier.height(14.dp))
        PrimaryButton(label = "Chia sẻ link", onClick = onShareAgain)
        // Image-share button stays disabled until the snapshot
        // re-read completes (one extra Firestore round trip after
        // publish — see ShareProgressViewModel).
        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            label = if (snapshot != null) "📸  Chia sẻ hình ảnh"
            else "Đang chuẩn bị hình ảnh…",
            enabled = snapshot != null,
            onClick = onShareImage
        )
    }
}

@Composable
private fun SecondaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = BetterMeColors.Primary.Primary
    val alpha = if (enabled) 1f else 0.45f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft * alpha))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = BetterMeTypography.Body.Medium,
            color = accent.copy(alpha = alpha),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LinkBlock(label: String, url: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = url,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextPrimary,
            maxLines = 2
        )
    }
}

@Composable
private fun ErrorBody(message: String, onRetry: () -> Unit) {
    Column {
        Text(
            text = "⚠️ $message",
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Red
        )
        Spacer(Modifier.height(12.dp))
        PrimaryButton(label = "Thử lại", onClick = onRetry)
    }
}

@Composable
private fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(BetterMeColors.Primary.Primary)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = BetterMeTypography.Body.Medium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun showToast(context: Context, message: String) {
    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
}

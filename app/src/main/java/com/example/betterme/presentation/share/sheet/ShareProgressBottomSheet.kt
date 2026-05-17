package com.example.betterme.presentation.share.sheet

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.betterme.domain.share.ShareType
import com.example.betterme.presentation.share.utils.ShareIntentHelper
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

/**
 * Bottom sheet entry point for the verified-share flow. Three slices:
 *
 *   ✅ Toàn bộ tiến độ      → ShareType.FULL_HISTORY
 *   📚 Một thói quen        → ShareType.HABIT     (needs habitId)
 *   🏆 Một thử thách        → ShareType.CHALLENGE (needs userChallengeId)
 *
 * The HABIT and CHALLENGE options surface here only when the caller
 * passed [preselectedItemId] (i.e. opened the sheet from inside a
 * habit/challenge detail screen). On the top-level Stats entry point
 * only "Full history" is offered, since the system doesn't know
 * which habit/challenge the user means.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareProgressBottomSheet(
    onDismiss: () -> Unit,
    preselectedType: ShareType? = null,
    preselectedItemId: String? = null,
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
                is ShareProgressEvent.ShowMessage -> {
                    showToast(context, event.message)
                }
            }
        }
    }

    // Auto-fire when launched with a preselected type — habit/challenge
    // detail screens pre-pick and just expect the share sheet to flash
    // briefly before launching the chooser.
    LaunchedEffect(preselectedType, preselectedItemId) {
        if (preselectedType != null) {
            viewModel.processIntent(
                ShareProgressIntent.CreateFor(preselectedType, preselectedItemId)
            )
        }
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
                ShareProgressUi.Picker -> PickerBody(
                    showHabitOption = preselectedType == ShareType.HABIT,
                    showChallengeOption = preselectedType == ShareType.CHALLENGE,
                    preselectedItemId = preselectedItemId,
                    onPick = { type, itemId ->
                        viewModel.processIntent(ShareProgressIntent.CreateFor(type, itemId))
                    }
                )
                ShareProgressUi.Generating -> GeneratingBody()
                is ShareProgressUi.Ready -> ReadyBody(
                    deepLink = ui.link.deepLink,
                    webLink = ui.link.webLink,
                    onShareAgain = {
                        ShareIntentHelper.shareText(
                            context = context,
                            subject = "Tiến độ BetterMe của tôi",
                            text = ui.link.richMessage
                        )
                    }
                )
                is ShareProgressUi.Error -> ErrorBody(
                    message = ui.message,
                    onRetry = {
                        viewModel.processIntent(ShareProgressIntent.Reset)
                    }
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
                text = "Chia sẻ tiến độ đã xác minh",
                style = BetterMeTypography.Title.Medium.Bold,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Người nhận chỉ thấy dữ liệu được xác nhận bởi máy chủ",
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
private fun PickerBody(
    showHabitOption: Boolean,
    showChallengeOption: Boolean,
    preselectedItemId: String?,
    onPick: (ShareType, String?) -> Unit
) {
    Column {
        PickerRow(
            emoji = "✅",
            title = "Toàn bộ tiến độ",
            subtitle = "Tất cả check-in của thói quen + thử thách (tối đa 500 lượt mới nhất)",
            onClick = { onPick(ShareType.FULL_HISTORY, null) }
        )
        if (showHabitOption) {
            Spacer(Modifier.height(8.dp))
            PickerRow(
                emoji = "📚",
                title = "Chỉ thói quen này",
                subtitle = "Toàn bộ check-in của thói quen đang xem",
                onClick = { onPick(ShareType.HABIT, preselectedItemId) }
            )
        }
        if (showChallengeOption) {
            Spacer(Modifier.height(8.dp))
            PickerRow(
                emoji = "🏆",
                title = "Chỉ thử thách này",
                subtitle = "Toàn bộ check-in của thử thách đang xem",
                onClick = { onPick(ShareType.CHALLENGE, preselectedItemId) }
            )
        }
    }
}

@Composable
private fun PickerRow(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .border(
                width = 1.dp,
                color = BetterMeColors.Border.BorderLight,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = emoji, fontSize = 22.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = BetterMeTypography.Body.Medium,
                color = BetterMeColors.Text.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
        Text(
            text = "›",
            color = BetterMeColors.Primary.Primary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GeneratingBody() {
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
            text = "Đang ký + lưu trữ snapshot trên máy chủ…",
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextSecondary
        )
    }
}

@Composable
private fun ReadyBody(
    deepLink: String,
    webLink: String,
    onShareAgain: () -> Unit
) {
    Column {
        Text(
            text = "✅  Link đã sẵn sàng — đã được máy chủ ký xác nhận.",
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Green,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))
        LinkBlock(label = "Link công khai", url = webLink)
        Spacer(Modifier.height(8.dp))
        LinkBlock(label = "Deep link (mở app)", url = deepLink)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                label = "Chia sẻ lại",
                onClick = onShareAgain,
                modifier = Modifier.weight(1f)
            )
        }
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

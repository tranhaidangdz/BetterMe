package com.example.betterme.presentation.habitdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.betterme.presentation.habitdetail.HabitDetailState
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Màn xác nhận check-in: hiển thị ảnh + ghi chú + thời gian + vị trí
 */
@Composable
fun CheckInConfirmSheet(
    state: HabitDetailState,
    onNoteChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BetterMeColors.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* block click through */ }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(BetterMeColors.White)
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            // ===== HEADER =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Xác nhận check-in",
                    style = BetterMeTypography.Title.Medium.Bold,
                    color = BetterMeColors.Text.TextPrimary
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BetterMeColors.Gray.Gray4)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        modifier = Modifier.size(20.dp),
                        tint = BetterMeColors.Text.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== PHOTO PREVIEW =====
            state.checkInPhotoUri?.let { uri ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            color = BetterMeColors.Border.BorderLight,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Ảnh check-in",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Timestamp overlay trên ảnh
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BetterMeColors.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        val timeStr = if (state.checkInTimestamp > 0) {
                            SimpleDateFormat("HH:mm • dd/MM/yyyy", Locale("vi"))
                                .format(Date(state.checkInTimestamp))
                        } else ""

                        Text(
                            text = "📸 $timeStr",
                            style = BetterMeTypography.Body.Small.Medium,
                            color = BetterMeColors.White
                        )
                    }

                    // Location overlay trên ảnh
                    if (state.checkInLatitude != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BetterMeColors.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = BetterMeColors.Green
                                )
                                Text(
                                    text = state.checkInLocationName
                                        ?: "${String.format("%.4f", state.checkInLatitude)}, ${String.format("%.4f", state.checkInLongitude)}",
                                    style = BetterMeTypography.Body.Small.Medium,
                                    color = BetterMeColors.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 140.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== TIME + LOCATION INFO =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = BetterMeColors.Primary.Primary
                    )
                    Text(
                        text = if (state.checkInTimestamp > 0) {
                            SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(Date(state.checkInTimestamp))
                        } else "—",
                        style = BetterMeTypography.Body.Medium,
                        color = BetterMeColors.Text.TextPrimary
                    )
                }

                // Location
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (state.checkInLatitude != null) BetterMeColors.Green
                        else BetterMeColors.Text.TextTertiary
                    )
                    Text(
                        text = if (state.checkInLatitude != null) "Đã xác định vị trí"
                        else "Đang lấy vị trí...",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = if (state.checkInLatitude != null) BetterMeColors.Green
                        else BetterMeColors.Text.TextTertiary,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== NOTE INPUT =====
            Text(
                text = "Ghi chú (tùy chọn)",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
            Spacer(modifier = Modifier.height(6.dp))
            TextField(
                value = state.checkInNote,
                onValueChange = onNoteChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                placeholder = {
                    Text(
                        "Hôm nay bạn cảm thấy thế nào?",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = BetterMeColors.Text.TextTertiary
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BetterMeColors.BackGround.BackgroundSecondary,
                    unfocusedContainerColor = BetterMeColors.BackGround.BackgroundSecondary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = BetterMeColors.Primary.Primary
                ),
                textStyle = BetterMeTypography.Body.Small.Medium,
                maxLines = 3,
                singleLine = false
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "${state.checkInNote.length}/200",
                    style = BetterMeTypography.Body.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== CONFIRM BUTTON =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (state.isSavingCheckIn) BetterMeColors.Primary.Primary.copy(alpha = 0.6f)
                        else BetterMeColors.Primary.Primary
                    )
                    .clickable(enabled = !state.isSavingCheckIn) { onConfirm() },
                contentAlignment = Alignment.Center
            ) {
                if (state.isSavingCheckIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = BetterMeColors.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "✓ Xác nhận check-in",
                        style = BetterMeTypography.Title.Small.Bold,
                        color = BetterMeColors.White
                    )
                }
            }
        }
    }
}

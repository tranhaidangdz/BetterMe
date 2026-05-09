package com.example.betterme.presentation.habitdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun AiSuggestTabContent(
    habitTitle: String,
    categoryName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // AI suggestion card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.03f)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(BetterMeColors.White)
                .padding(16.dp)
        ) {
            Text(
                text = "🤖 Gợi ý từ AI",
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Dựa trên thói quen \"$habitTitle\" thuộc nhóm \"$categoryName\", AI gợi ý cho bạn:",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
            Spacer(modifier = Modifier.height(12.dp))

            SuggestionItem(
                emoji = "💡",
                text = "Hãy đặt nhắc nhở vào cùng một giờ mỗi ngày để tạo thói quen ổn định."
            )
            Spacer(modifier = Modifier.height(8.dp))
            SuggestionItem(
                emoji = "🎯",
                text = "Bắt đầu từ mục tiêu nhỏ rồi tăng dần. Điều quan trọng nhất là duy trì chuỗi ngày liên tục."
            )
            Spacer(modifier = Modifier.height(8.dp))
            SuggestionItem(
                emoji = "🤝",
                text = "Chia sẻ tiến trình với bạn bè hoặc người thân để tạo động lực duy trì."
            )
        }

        // Related habits
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.03f)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(BetterMeColors.White)
                .padding(16.dp)
        ) {
            Text(
                text = "📋 Thói quen liên quan",
                style = BetterMeTypography.Title.Small.Bold,
                color = BetterMeColors.Text.TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Một số thói quen bạn có thể kết hợp cùng nhóm \"$categoryName\":",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
            Spacer(modifier = Modifier.height(12.dp))

            RelatedHabitItem(emoji = "🏃", text = "Tập thể dục nhẹ 15 phút mỗi sáng")
            Spacer(modifier = Modifier.height(6.dp))
            RelatedHabitItem(emoji = "🧘", text = "Thiền 5 phút trước khi ngủ")
            Spacer(modifier = Modifier.height(6.dp))
            RelatedHabitItem(emoji = "📖", text = "Viết nhật ký 3 điều biết ơn mỗi tối")
        }
    }
}

@Composable
private fun SuggestionItem(emoji: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = emoji, style = BetterMeTypography.Body.Medium)
        Text(
            text = text,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextPrimary
        )
    }
}

@Composable
private fun RelatedHabitItem(emoji: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, style = BetterMeTypography.Body.Medium)
        Text(
            text = text,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextPrimary
        )
    }
}

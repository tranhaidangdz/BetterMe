package com.example.betterme.presentation.categorydetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Action row at the bottom of the Habit Group screen. White surface, accent-tinted
 * border, neutral shadow — same grammar as HabitDetailCard so the screen reads as one
 * vertical rhythm of rows. Accent identity lives in the icon disc and chevron pill;
 * the body stays neutral so the label is fully readable on every category.
 */
@Composable
fun CategoryActionButton(
    icon: String,
    label: String,
    onClick: () -> Unit,
    accentColor: Color = BetterMeColors.Primary.Primary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = BetterMeTokens.CardElevation.Body,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body),
                ambientColor = BetterMeTokens.NeutralShadow.Ambient,
                spotColor = BetterMeTokens.NeutralShadow.Spot
            )
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Body))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Body)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = BetterMeTokens.AccentAlpha.Soft)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 18.sp)
        }
        Text(
            text = label,
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
                .background(accentColor.copy(alpha = BetterMeTokens.AccentAlpha.Soft))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "›",
                style = BetterMeTypography.Title.Small.Bold,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

package com.example.betterme.presentation.addhabit.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.data.local.room.entities.CategoryEntity
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTokens
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Horizontal scrollable row of category chips. Replaces the prior collapsible
 * CategorySelector dropdown — much faster to scan and pick on mobile.
 *
 * Each chip is a tinted-disc icon + label inside an outlined pill. Selected state
 * springs subtly (scale 1.0 → 1.05) so the tap registers visually without a heavy
 * animation. Tokens come from BetterMeTokens so the chip alpha scale matches every
 * other surface in the redesigned app.
 */
@Composable
fun CategoryChipRow(
    categories: List<CategoryEntity>,
    selectedCategoryId: Int?,
    accent: Color,
    onSelect: (id: Int, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Standard))
                .background(BetterMeColors.Gray.Gray3),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Chưa có nhóm thói quen nào",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Text.TextTertiary
            )
        }
        return
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            CategoryChip(
                emoji = category.icon,
                label = category.name,
                isSelected = category.id == selectedCategoryId,
                accent = accent,
                onClick = { onSelect(category.id, category.name) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    emoji: String,
    label: String,
    isSelected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chip_scale"
    )
    val (bg, border, text) = if (isSelected) {
        Triple(
            accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft),
            accent,
            accent
        )
    } else {
        Triple(
            Color.White,
            accent.copy(alpha = BetterMeTokens.AccentAlpha.Medium),
            BetterMeColors.Text.TextPrimary
        )
    }

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(BetterMeTokens.CardRadius.Pill))
            .background(bg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = border,
                shape = RoundedCornerShape(BetterMeTokens.CardRadius.Pill)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = BetterMeTokens.AccentAlpha.Soft)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 14.sp)
        }
        Spacer(Modifier.size(8.dp))
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = text,
            fontWeight = FontWeight.SemiBold
        )
    }
}

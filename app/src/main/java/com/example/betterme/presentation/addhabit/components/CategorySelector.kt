package com.example.betterme.presentation.addhabit.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.data.local.room.entities.CategoryEntity
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun CategorySelector(
    categories: List<CategoryEntity>,
    selectedCategoryName: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (categoryId: Int, categoryName: String) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = BetterMeColors.BackGround.BackgroundPrimary
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Nhóm thói quen",
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Dropdown trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(containerColor)
                .border(1.dp, BetterMeColors.Border.BorderLight, RoundedCornerShape(14.dp))
                .clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedCategoryName.ifBlank { "Chọn nhóm thói quen" },
                style = BetterMeTypography.Body.Medium,
                color = if (selectedCategoryName.isBlank())
                    BetterMeColors.Text.TextTertiary
                else
                    BetterMeColors.Text.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded)
                    Icons.Default.KeyboardArrowUp
                else
                    Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = BetterMeColors.Text.TextTertiary
            )
        }

        // Dropdown content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(containerColor)
                    .border(1.dp, BetterMeColors.Border.BorderLight, RoundedCornerShape(14.dp))
            ) {
                categories.forEach { category ->
                    val isSelected = category.name == selectedCategoryName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(category.id, category.name) }
                            .background(
                                if (isSelected)
                                    BetterMeColors.Primary.Primary.copy(alpha = 0.08f)
                                else
                                    containerColor
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = category.icon,
                            fontSize = 20.sp
                        )
                        Text(
                            text = category.name,
                            style = BetterMeTypography.Body.Medium,
                            color = if (isSelected)
                                BetterMeColors.Primary.Primary
                            else
                                BetterMeColors.Text.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

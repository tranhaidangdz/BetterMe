package com.example.betterme.presentation.dailyhabits.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.dailyhabits.DateUiModel
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun DateSelector(
    dates: List<DateUiModel>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(dates.indices.toList()) { index ->
            val date = dates[index]
            val isSelected = index == selectedIndex
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) BetterMeColors.Primary.Primary
                        else BetterMeColors.Gray.Gray3
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = date.month,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = if (isSelected) BetterMeColors.White else BetterMeColors.Text.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = date.day,
                    style = BetterMeTypography.Title.Medium.Bold,
                    color = if (isSelected) BetterMeColors.White else BetterMeColors.Text.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = date.weekDay,
                    style = BetterMeTypography.Body.Small.Medium,
                    color = if (isSelected) BetterMeColors.White else BetterMeColors.Text.TextTertiary
                )
            }
        }
    }
}

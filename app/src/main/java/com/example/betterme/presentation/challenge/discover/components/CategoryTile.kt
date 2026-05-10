package com.example.betterme.presentation.challenge.discover.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.betterme.presentation.challenge.model.CategoryTileUi
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun CategoryTile(
    model: CategoryTileUi,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0D000000),
                spotColor = Color(0x1A000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(model.accentColor.copy(alpha = 0.12f))
            .clickable { onClick(model.categoryId) }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!model.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = model.imageUrl,
                contentDescription = model.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(36.dp)
                    .fillMaxWidth()
            )
        } else {
            Text(text = model.emoji, fontSize = 28.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = model.name,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextPrimary,
            maxLines = 1
        )
        Text(
            text = "${model.challengeCount} thử thách",
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextTertiary
        )
    }
}

package com.example.betterme.presentation.components.view

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.example.betterme.R
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeShapes
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun UploadImageSection(
    imagePath: Uri?,
    onUploadImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(200.dp)
            .clip(BetterMeShapes.extraLarge)
            .background(BetterMeColors.BackGround.BackgroundPrimary)
            .border(
                width = 2.dp,
                color = BetterMeColors.Primary.Primary,
                shape = BetterMeShapes.extraLarge
            )
            .clickable {
                onUploadImageClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (imagePath == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_upload_image),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(36.dp)
                )

                Text(
                    text = stringResource(R.string.upload_image),
                    style = BetterMeTypography.Title.Small.SemiBold,
                    color = BetterMeColors.Text.TextTertiary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imagePath)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .clip(BetterMeShapes.large)
                    .align(Alignment.Center)
                    .aspectRatio(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UploadImageSectionPreview() {
    UploadImageSection(
        imagePath = null,
        onUploadImageClick = {}
    )
}
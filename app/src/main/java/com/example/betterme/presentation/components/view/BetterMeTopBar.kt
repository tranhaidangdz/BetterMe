package com.example.betterme.presentation.components.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.betterme.R
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import com.example.betterme.utils.ext.rawClickable

@Composable
fun BetterMeTopBar(
    leadingIconRes: Int,
    title: String,
    onLeadingClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIconRes: Int? = null,
    onTrailingClick: () -> Unit = {},
    backgroundColor: Color = BetterMeColors.BackGround.BackgroundSecondary,
    iconTint: Color = BetterMeColors.Black,
    titleColor: Color = BetterMeColors.Text.TextPrimary
) {
    Row(
        modifier = modifier
            .background(backgroundColor)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .rawClickable { onLeadingClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(leadingIconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = title,
            style = BetterMeTypography.Title.Medium.Bold,
            color = titleColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        if (trailingIconRes != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .rawClickable { onTrailingClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(trailingIconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Box(Modifier.size(40.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BetterMeTopBarPreview() {
    BetterMeTopBar(
        leadingIconRes = R.drawable.ic_arrow_left,
        trailingIconRes = null,
        title = "Add habit",
        onLeadingClick = { },
        onTrailingClick = { }
    )
}

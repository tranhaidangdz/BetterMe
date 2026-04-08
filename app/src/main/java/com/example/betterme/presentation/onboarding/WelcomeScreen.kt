package com.example.betterme.presentation.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import com.example.betterme.R
import com.example.betterme.presentation.components.button.BetterMeButton


@Composable
fun WelcomeScreen(
    navigateToOnboarding: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundPrimary)
            .systemBarsPadding() // 👈 FIX tràn status + nav bar
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.img_welcome),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = stringResource(R.string.welcome_title) + " ",
                style = BetterMeTypography.Headline.Medium.Bold,
                color = BetterMeColors.Text.TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.app_name),
                style = BetterMeTypography.Headline.Medium.Bold,
                color = BetterMeColors.Primary.Primary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.welcome_description),
            style = BetterMeTypography.Title.Medium.SemiBold,
            color = BetterMeColors.Text.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        BetterMeButton(
            onClick = navigateToOnboarding,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(56.dp)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(), // 👈 FIX bị đè nút,
            style = BetterMeTypography.Title.Large.SemiBold.copy(
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    WelcomeScreen(
        navigateToOnboarding = {}
    )
}
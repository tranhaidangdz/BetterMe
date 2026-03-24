package com.example.betterme.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.betterme.R
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import org.koin.androidx.compose.koinViewModel

@Composable
fun SplashScreen(
    navigateToWelcome: () -> Unit,
    navigateToMain: () -> Unit,
    navigateToSignIn: () -> Unit,
    viewModel: SplashViewModel = koinViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.processIntent(SplashIntent.CheckFirstLaunch)
    }

    LaunchedEffect(Unit) {
        viewModel.singleEvent.collect { event ->
            when (event) {
                SplashEvent.NavigateToWelcome -> {
                    navigateToWelcome()
                }

                SplashEvent.NavigateToMain -> {
                    navigateToMain()
                }

                SplashEvent.NavigateToSignIn -> {
                    navigateToSignIn()
                }
            }
        }
    }

    SplashScreenContent()
}

@Composable
fun SplashScreenContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundPrimary)
            .padding(bottom = 50.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_logo_round),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(150.dp)
        )

        Text(
            text = stringResource(R.string.app_name),
            style = BetterMeTypography.Headline.Large.Bold.copy(
                shadow = Shadow(
                    color = BetterMeColors.Primary.Primary.copy(alpha = 0.7f),
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            ),
            color = BetterMeColors.Primary.Primary
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    SplashScreenContent()
}
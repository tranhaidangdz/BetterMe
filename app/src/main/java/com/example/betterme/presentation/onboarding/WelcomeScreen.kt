package com.example.betterme.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    navigateToOnboarding: () -> Unit,
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image Section with subtle fade in
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(800)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_welcome),
                        contentDescription = "Welcome Illustration",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )
                }
            }

            // Bottom Content Section with slide up and fade
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(800)) + fadeIn(animationSpec = tween(800)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.8f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .shadow(
                            elevation = 16.dp, 
                            shape = RoundedCornerShape(32.dp), 
                            spotColor = BetterMeColors.Primary.Primary.copy(alpha = 0.15f)
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.welcome_title) + " ",
                            style = BetterMeTypography.Headline.Medium.Bold,
                            color = BetterMeColors.Text.TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = BetterMeTypography.Headline.Medium.Bold,
                            color = BetterMeColors.Primary.Primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.welcome_description),
                        style = BetterMeTypography.Title.Medium.SemiBold,
                        color = BetterMeColors.Text.TextPrimary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    BetterMeButton(
                        onClick = navigateToOnboarding,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                elevation = 8.dp, 
                                shape = RoundedCornerShape(16.dp), 
                                spotColor = BetterMeColors.Primary.Primary.copy(alpha = 0.3f)
                            ),
                        style = BetterMeTypography.Title.Large.SemiBold.copy(
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    WelcomeScreen(
        navigateToOnboarding = {}
    )
}
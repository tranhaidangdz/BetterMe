package com.example.betterme.presentation.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.betterme.R
import com.example.betterme.presentation.components.button.BetterMeButton
import com.example.betterme.presentation.onboarding.model.OnboardingPage
import com.example.betterme.presentation.onboarding.model.getOnboardingPages
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    navigateToHabitSelection: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val pages = getOnboardingPages()
    val pagerState = rememberPagerState(pageCount = { pages.size })

    LaunchedEffect(Unit) {
        viewModel.singleEvent.collectLatest { event ->
            when (event) {
                OnboardingEvent.NavigateToHabitSelection -> navigateToHabitSelection()
            }
        }
    }

    OnboardingContent(
        pages = pages,
        pagerState = pagerState,
        onIntent = viewModel::processIntent
    )
}

@Composable
fun OnboardingContent(
    pages: List<OnboardingPage>,
    pagerState: PagerState,
    onIntent: (OnboardingIntent) -> Unit
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundPrimary)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ===== IMAGE =====
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.2f), // 👈 ảnh chiếm nhiều hơn
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = pages[page].imageResource),
                            contentDescription = null,
                            contentScale = ContentScale.Fit, // 👈 FIX quan trọng
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(pages[page].title),
                        style = BetterMeTypography.Headline.Large.Bold,
                        color = BetterMeColors.Text.TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(pages[page].description),
                        style = BetterMeTypography.Body.Large.Medium,
                        color = BetterMeColors.Text.TextTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .padding(bottom = 180.dp)
                    .height(20.dp)
                    .background(
                        BetterMeColors.BackGround.BackgroundOnboardingDots,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(start = 12.dp, end = 4.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val color = if (pagerState.currentPage == iteration) {
                        BetterMeColors.Primary.Primary
                    } else BetterMeColors.Border.BorderDropDown
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                            .padding(vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        BetterMeButton(
            onClick = {
                if (pagerState.currentPage < pages.size - 1) {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                } else {
                    onIntent(OnboardingIntent.NavigateToHabitSelection)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .heightIn(56.dp),
            style = BetterMeTypography.Title.Large.SemiBold.copy(
                color = Color.White
            ),
            text = stringResource(R.string.button_continue)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    val pages = getOnboardingPages()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    OnboardingContent(
        pages = pages,
        pagerState = pagerState,
        onIntent = { }
    )
}
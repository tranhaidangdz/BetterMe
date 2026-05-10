package com.example.betterme.presentation.challenge.upcoming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.betterme.R
import com.example.betterme.presentation.challenge.overview.ChallengeOverviewIntent
import com.example.betterme.presentation.challenge.overview.ChallengeOverviewViewModel
import com.example.betterme.presentation.challenge.overview.components.UpcomingChallengeRow
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChallengeUpcomingScreen(
    onBackClick: () -> Unit,
    onOpenChallengeDetail: (Int) -> Unit,
    viewModel: ChallengeOverviewViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundSecondary)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BetterMeTopBar(
                    leadingIconRes = R.drawable.ic_arrow_left,
                    title = "Sắp diễn ra",
                    onLeadingClick = onBackClick
                )
            }
            if (state.upcoming.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có thử thách sắp diễn ra",
                            style = BetterMeTypography.Body.Medium,
                            color = BetterMeColors.Text.TextTertiary
                        )
                    }
                }
            } else {
                items(state.upcoming, key = { it.challengeId }) { row ->
                    UpcomingChallengeRow(
                        model = row,
                        onToggleReminder = { viewModel.processIntent(ChallengeOverviewIntent.ToggleStartReminder(it)) },
                        onClick = onOpenChallengeDetail,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

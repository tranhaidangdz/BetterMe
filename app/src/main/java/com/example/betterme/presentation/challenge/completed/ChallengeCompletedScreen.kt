package com.example.betterme.presentation.challenge.completed

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.betterme.R
import com.example.betterme.presentation.challenge.model.CompletedFilter
import com.example.betterme.presentation.challenge.overview.ChallengeOverviewViewModel
import com.example.betterme.presentation.challenge.overview.components.CompletedChallengeRow
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.components.view.PillSegmentedTabs
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChallengeCompletedScreen(
    onBackClick: () -> Unit,
    onOpenChallengeDetail: (Int) -> Unit,
    viewModel: ChallengeOverviewViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()
    var subFilter by remember { mutableStateOf(CompletedFilter.All) }

    val rows = remember(subFilter, state.completed) {
        when (subFilter) {
            CompletedFilter.All -> state.completed
            CompletedFilter.Done -> state.completed.filter { it.isCompleted }
            // Strict "FAILED only" — ABANDONED rows show up in All but not here, since
            // abandonment is a user action while FAILED is the consequence of missing
            // a required day. The user asked for these to be visually distinguishable.
            CompletedFilter.Failed -> state.completed.filter { it.isFailed }
        }
    }

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
                    title = "Lịch sử thử thách",
                    onLeadingClick = onBackClick
                )
            }
            item {
                PillSegmentedTabs(
                    items = CompletedFilter.entries,
                    selected = subFilter,
                    label = { it.label },
                    onSelect = { subFilter = it },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            if (rows.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có dữ liệu",
                            style = BetterMeTypography.Body.Medium,
                            color = BetterMeColors.Text.TextTertiary
                        )
                    }
                }
            } else {
                items(rows, key = { it.userChallengeId }) { row ->
                    CompletedChallengeRow(
                        model = row,
                        onClick = onOpenChallengeDetail,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

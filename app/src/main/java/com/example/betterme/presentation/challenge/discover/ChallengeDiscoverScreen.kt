package com.example.betterme.presentation.challenge.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.betterme.R
import com.example.betterme.presentation.challenge.discover.components.CategoryFilterChips
import com.example.betterme.presentation.challenge.discover.components.CategoryTile
import com.example.betterme.presentation.challenge.discover.components.DiscoverSearchBar
import com.example.betterme.presentation.challenge.discover.components.FeaturedChallengeCard
import com.example.betterme.presentation.challenge.discover.components.NewChallengeRow
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.components.view.SectionHeader
import com.example.betterme.presentation.theme.BetterMeColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChallengeDiscoverScreen(
    onBackClick: () -> Unit,
    onOpenChallengeDetail: (Int) -> Unit,
    onOpenGroupChallenge: (Int) -> Unit,
    viewModel: ChallengeDiscoverViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()

    val routeOpen: (Int) -> Unit = { id ->
        val featured = state.featured.firstOrNull { it.challengeId == id }
        val newest = state.newest.firstOrNull { it.challengeId == id }
        val search = state.searchResults.firstOrNull { it.challengeId == id }
        val isGroup = featured?.isGroup ?: newest?.isGroup ?: search?.isGroup ?: false
        if (isGroup) onOpenGroupChallenge(id) else onOpenChallengeDetail(id)
    }

    val visibleNewest = if (state.query.isBlank()) {
        if (state.selectedCategoryId == null) state.newest
        else state.newest.filter { challenge ->
            // We don't have category id on NewChallengeUiModel; rely on the searchResults path.
            true
        }
    } else state.searchResults

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
                    title = "Khám phá thử thách",
                    onLeadingClick = onBackClick
                )
            }
            item {
                DiscoverSearchBar(
                    query = state.query,
                    onQueryChange = { viewModel.processIntent(ChallengeDiscoverIntent.UpdateQuery(it)) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item {
                CategoryFilterChips(
                    categories = state.categories,
                    selectedId = state.selectedCategoryId,
                    onSelect = { viewModel.processIntent(ChallengeDiscoverIntent.SelectCategory(it)) }
                )
            }

            if (state.featured.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Nổi bật",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(state.featured, key = { it.challengeId }) { card ->
                            FeaturedChallengeCard(
                                model = card,
                                onClick = routeOpen
                            )
                        }
                    }
                }
            }

            if (state.categories.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Theo danh mục",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                items(state.categories.chunked(3), key = { it.firstOrNull()?.categoryId ?: 0 }) { row ->
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { tile ->
                            CategoryTile(
                                model = tile,
                                onClick = { id -> viewModel.processIntent(ChallengeDiscoverIntent.SelectCategory(id)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Pad the row if fewer than 3 items.
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = if (state.query.isNotBlank()) "Kết quả" else "Mới",
                    trailingLabel = if (state.query.isBlank()) "Xem tất cả" else null,
                    onTrailingClick = { /* future */ },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            items(visibleNewest, key = { it.challengeId }) { row ->
                NewChallengeRow(
                    model = row,
                    onClick = routeOpen,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            if (visibleNewest.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(40.dp)
                    ) {
                        Text(
                            text = if (state.query.isNotBlank()) "Không tìm thấy kết quả" else "Chưa có thử thách nào",
                            color = BetterMeColors.Text.TextTertiary
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

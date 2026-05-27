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
import com.example.betterme.presentation.challenge.discover.components.CategoryTile
import com.example.betterme.presentation.challenge.discover.components.DifficultyFilterChips
import com.example.betterme.presentation.challenge.discover.components.DifficultySectionHeader
import com.example.betterme.presentation.challenge.discover.components.DiscoverSearchBar
import com.example.betterme.presentation.challenge.discover.components.FeaturedChallengeCard
import com.example.betterme.presentation.challenge.discover.components.LegendaryChallengeRow
import com.example.betterme.presentation.challenge.discover.components.NewChallengeRow
import com.example.betterme.presentation.challenge.shared.Difficulty
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

    // Pick the source list based on whether the user is searching, then apply the
    // difficulty filter on top. (Category filter still routes through the VM's
    // search field; we don't have category_id on the UI model so we keep the original
    // behaviour for that.)
    val visibleNewest = (if (state.query.isBlank()) state.newest else state.searchResults)
        .let { list ->
            val d = state.selectedDifficulty
            if (d == null) list else list.filter { it.difficulty == d }
        }
    val visibleFeatured = state.featured // featured doesn't carry difficulty in the UI model

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
                DifficultyFilterChips(
                    selected = state.selectedDifficulty,
                    onSelect = { viewModel.processIntent(ChallengeDiscoverIntent.SelectDifficulty(it)) }
                )
            }
            // The "challenges by habit group" category chip row that used to sit
            // here was removed per design — category browsing still lives in the
            // "Theo danh mục" tile grid below, and SelectCategory filtering is
            // unchanged (the tiles dispatch the same intent).

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

            if (state.upcomingFeatured.isNotEmpty()) {
                item {
                    com.example.betterme.presentation.components.view.SectionHeader(
                        title = "Sắp diễn ra",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(state.upcomingFeatured, key = { it.challengeId }) { card ->
                            com.example.betterme.presentation.challenge.discover.components.UpcomingFeatureCard(
                                title = card.title,
                                iconEmoji = card.iconEmoji,
                                accentColor = card.accentColor,
                                daysUntilStart = card.daysUntilStart,
                                startLabel = card.startLabel,
                                rewardCoins = card.rewardCoins,
                                participantCount = card.participantCount,
                                onClick = { onOpenChallengeDetail(card.challengeId) }
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
                // Prefix keys with "cat-row-" so they cannot collide with the integer
                // challenge IDs used by the "Mới" items() block below — LazyColumn requires
                // every key in the same scroll container to be globally unique, otherwise
                // it crashes with "Two items used the same key".
                items(
                    state.categories.chunked(3),
                    key = { row -> "cat-row-${row.firstOrNull()?.categoryId ?: 0}" }
                ) { row ->
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

            // ───────── Grouped-by-difficulty showcase ─────────
            // Only surfaced when the user hasn't filtered or searched —
            // becomes a passive ladder browser rather than fighting the
            // flat list below. LEGENDARY rows use a prestige-styled
            // variant; the other tiers reuse the standard NewChallengeRow.
            val showGrouped = state.query.isBlank() &&
                state.selectedDifficulty == null &&
                state.selectedCategoryId == null &&
                state.groupedByDifficulty.isNotEmpty()
            if (showGrouped) {
                item {
                    SectionHeader(
                        title = "Theo độ khó",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                // Render in tier order EASY → MEDIUM → HARD → LEGENDARY so
                // the visual escalation matches the user's progression
                // intuition.
                Difficulty.entries.forEach { tier ->
                    val rows = state.groupedByDifficulty[tier] ?: return@forEach
                    item(key = "tier-header-${tier.raw}") {
                        DifficultySectionHeader(difficulty = tier, count = rows.size)
                    }
                    items(rows, key = { row -> "tier-${tier.raw}-${row.challengeId}" }) { row ->
                        if (tier == Difficulty.LEGENDARY) {
                            LegendaryChallengeRow(
                                model = row,
                                onClick = routeOpen,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        } else {
                            NewChallengeRow(
                                model = row,
                                onClick = routeOpen,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
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
            items(visibleNewest, key = { "new-${it.challengeId}" }) { row ->
                if (row.difficulty == Difficulty.LEGENDARY) {
                    LegendaryChallengeRow(
                        model = row,
                        onClick = routeOpen,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    NewChallengeRow(
                        model = row,
                        onClick = routeOpen,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
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

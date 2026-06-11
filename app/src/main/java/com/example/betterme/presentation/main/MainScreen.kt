package com.example.betterme.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.addhabit.AddHabitScreen
import com.example.betterme.presentation.categorydetail.CategoryDetailEvent
import com.example.betterme.presentation.categorydetail.CategoryDetailIntent
import com.example.betterme.presentation.categorydetail.CategoryDetailScreen
import com.example.betterme.presentation.categorydetail.CategoryDetailViewModel
import com.example.betterme.presentation.challenge.achievements.ChallengeAchievementsScreen
import com.example.betterme.presentation.challenge.badges.ChallengeBadgesScreen
import com.example.betterme.presentation.challenge.completed.ChallengeCompletedScreen
import com.example.betterme.presentation.challenge.detail.ChallengeDetailScreen
import com.example.betterme.presentation.leaderboard.LeaderboardScreen
import com.example.betterme.presentation.leaderboard.global.GlobalLeaderboardScreen
import com.example.betterme.presentation.share.profile.PublicProfileScreen
import com.example.betterme.presentation.share.sheet.ShareProgressBottomSheet
import com.example.betterme.presentation.share.viewer.ShareViewerScreen
import com.example.betterme.presentation.challenge.discover.ChallengeDiscoverScreen
import com.example.betterme.presentation.challenge.group.ChallengeGroupScreen
import com.example.betterme.presentation.challenge.overview.ChallengeOverviewScreen
import com.example.betterme.presentation.challenge.upcoming.ChallengeUpcomingScreen
import com.example.betterme.presentation.dailyhabits.DailyHabitsScreen
import com.example.betterme.presentation.habitdetail.HabitDetailScreen
import com.example.betterme.presentation.home.HomeScreen
import com.example.betterme.presentation.main.components.BottomNavBar
import com.example.betterme.presentation.statistics.StatisticsScreen
import com.example.betterme.presentation.main.model.MainTab
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import com.example.betterme.utils.DeepLinkBus
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun MainScreen(
    navigateToSettings: () -> Unit,
    navigateToSignIn: () -> Unit,
    viewModel: MainViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()

    val deepLinkBus = koinInject<DeepLinkBus>()
    LaunchedEffect(Unit) {
        deepLinkBus.events.collect { event ->
            when (event) {
                is DeepLinkBus.Event.OpenUserChallenge ->
                    viewModel.processIntent(
                        MainIntent.OpenChallengeDetail(event.userChallengeId, isPreview = false)
                    )
                is DeepLinkBus.Event.OpenChallengePreview ->
                    viewModel.processIntent(
                        MainIntent.OpenChallengeDetail(event.challengeId, isPreview = true)
                    )
                is DeepLinkBus.Event.OpenHabitDetail ->
                    viewModel.processIntent(MainIntent.OpenHabitDetail(event.habitId))
                is DeepLinkBus.Event.OpenShareViewer ->
                    viewModel.processIntent(MainIntent.OpenShareViewer(event.userId))
                is DeepLinkBus.Event.OpenPublicProfile ->
                    viewModel.processIntent(MainIntent.OpenPublicProfile(event.userId))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Content area
        when (state.selectedTab) {
            MainTab.HOME -> HomeScreen(
                refreshVersion = state.homeRefreshVersion,
                onLogoutSuccess = navigateToSignIn,
                onViewProgress = {
                    viewModel.processIntent(MainIntent.SelectTab(MainTab.HABITS))
                },
                onCategoryClick = { categoryId, categoryName, categoryIcon ->
                    viewModel.processIntent(
                        MainIntent.OpenCategoryDetail(
                            categoryId = categoryId,
                            categoryName = categoryName,
                            categoryIcon = categoryIcon
                        )
                    )
                },
                onHabitClick = { habitId ->
                    viewModel.processIntent(MainIntent.OpenHabitDetail(habitId))
                },
                onChallengeDetailClick = { ucId ->
                    viewModel.processIntent(MainIntent.OpenChallengeDetail(ucId, isPreview = false))
                },
                onChallengePreviewClick = { challengeId ->
                    viewModel.processIntent(MainIntent.OpenChallengeDetail(challengeId, isPreview = true))
                }
            )
            MainTab.HABITS -> DailyHabitsScreen(
                onBackClick = { viewModel.processIntent(MainIntent.SelectTab(MainTab.HOME)) },
                onHabitClick = { habitId ->
                    viewModel.processIntent(MainIntent.OpenHabitDetail(habitId))
                }
            )
            MainTab.ADD -> AddHabitScreen(
                onHabitAdded = { viewModel.processIntent(MainIntent.HabitAdded) },
                onBackClick = { viewModel.processIntent(MainIntent.SelectTab(MainTab.HOME)) }
            )
            MainTab.CHALLENGE -> ChallengeOverviewScreen(
                onOpenChallengeDetail = { id ->
                    viewModel.processIntent(MainIntent.OpenChallengeDetail(id))
                },
                onOpenDiscover = {
                    viewModel.processIntent(MainIntent.OpenChallengeDiscover)
                },
                onOpenAchievements = {
                    viewModel.processIntent(MainIntent.OpenChallengeAchievements)
                },
                onOpenUpcoming = {
                    viewModel.processIntent(MainIntent.OpenChallengeUpcoming)
                },
                onOpenCompleted = {
                    viewModel.processIntent(MainIntent.OpenChallengeCompleted)
                },
                onOpenGlobalLeaderboard = {
                    viewModel.processIntent(MainIntent.OpenGlobalLeaderboard)
                }
            )
            MainTab.STATS -> StatisticsScreen(
                onBackClick = { viewModel.processIntent(MainIntent.SelectTab(MainTab.HOME)) },
                // Tapping any row in the Completed / Failed / Ongoing sections opens
                // that habit's full history (HabitDetailScreen) as an overlay. Logs are
                // permanent in Room — completed and failed habits both display their
                // full historical check-in calendar + log list.
                onHabitClick = { habitId ->
                    viewModel.processIntent(MainIntent.OpenHabitDetail(habitId))
                },
                onOpenShare = {
                    viewModel.processIntent(MainIntent.OpenShareProgressSheet)
                }
            )
        }

        // Category Detail Overlay
        if (state.categoryDetailId != null) {
            val categoryDetailViewModel = koinViewModel<CategoryDetailViewModel>()
            val detailState by categoryDetailViewModel.viewState.collectAsState()
            val currentCategoryId = state.categoryDetailId
            val snackbarHostState = remember { SnackbarHostState() }

            if (currentCategoryId != null) {
                LaunchedEffect(currentCategoryId) {
                    categoryDetailViewModel.processIntent(
                        CategoryDetailIntent.LoadData(
                            categoryId = currentCategoryId,
                            categoryName = state.categoryDetailName,
                            categoryIcon = state.categoryDetailIcon
                        )
                    )
                }
            }

            // Single-shot events from the Category VM: a habit was added from an AI
            // suggestion → show a snackbar so the user feels the action landed.
            LaunchedEffect(categoryDetailViewModel) {
                categoryDetailViewModel.singleEvent.collect { event ->
                    when (event) {
                        is CategoryDetailEvent.HabitAddedFromSuggestion ->
                            snackbarHostState.showSnackbar("✅ Đã thêm \"${event.title}\" vào nhóm")
                        else -> Unit
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                CategoryDetailScreen(
                    state = detailState,
                    onBack = { viewModel.processIntent(MainIntent.CloseCategoryDetail) },
                    onHabitClick = { habitId ->
                        viewModel.processIntent(MainIntent.OpenHabitDetail(habitId))
                    },
                    onAiReviewClick = {
                        // First tap: cache-first (no network if a fresh entry exists).
                        // The card's "Tạo lại" calls onAiReviewRegenerate which forces.
                        categoryDetailViewModel.processIntent(
                            CategoryDetailIntent.GenerateAiReview(forceRefresh = false)
                        )
                    },
                    onAiReviewRegenerate = {
                        categoryDetailViewModel.processIntent(
                            CategoryDetailIntent.GenerateAiReview(forceRefresh = true)
                        )
                    },
                    onDismissAiReview = {
                        categoryDetailViewModel.processIntent(
                            CategoryDetailIntent.DismissAiReview
                        )
                    },
                    onAddHabitClick = {
                        viewModel.processIntent(MainIntent.CloseCategoryDetail)
                        viewModel.processIntent(MainIntent.SelectTab(MainTab.ADD))
                    },
                    onAiSuggestClick = {
                        categoryDetailViewModel.processIntent(
                            CategoryDetailIntent.GenerateAiSuggestions(forceRefresh = false)
                        )
                    },
                    onAiSuggestRegenerate = {
                        categoryDetailViewModel.processIntent(
                            CategoryDetailIntent.GenerateAiSuggestions(forceRefresh = true)
                        )
                    },
                    onDismissAiSuggestions = {
                        categoryDetailViewModel.processIntent(
                            CategoryDetailIntent.DismissAiSuggestions
                        )
                    },
                    onAddAiSuggestion = { suggestion ->
                        categoryDetailViewModel.processIntent(
                            CategoryDetailIntent.AddAiSuggestion(suggestion)
                        )
                    }
                )

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp)
                )
            }
        }

        // Habit Detail Overlay
        if (state.habitDetailId != null) {
            HabitDetailScreen(
                habitId = state.habitDetailId!!,
                onBackClick = { viewModel.processIntent(MainIntent.CloseHabitDetail) }
            )
        }

        // ===== Overlay z-order =====
        // The list-type overlays (Discover, Achievements, Badges, Group, Upcoming,
        // Completed) MUST render before the Challenge Detail block. Inside a Compose
        // [Box], children draw in source order — the last `if`-block sits on top.
        // Challenge Detail is opened FROM these list overlays, so it must be the
        // topmost screen; otherwise tapping a card mounts Detail behind the open list
        // and the user only sees it after dismissing the list (the "press back to see
        // detail" bug). Keep Challenge Detail last.

        // Challenge Achievements Profile Overlay
        if (state.showChallengeAchievements) {
            ChallengeAchievementsScreen(
                onBackClick = { viewModel.processIntent(MainIntent.CloseChallengeAchievements) },
                onSeeAllBadges = { viewModel.processIntent(MainIntent.OpenChallengeBadges) }
            )
        }

        // Challenge Badges Overlay
        if (state.showChallengeBadges) {
            ChallengeBadgesScreen(
                onBackClick = { viewModel.processIntent(MainIntent.CloseChallengeBadges) }
            )
        }

        // Challenge Group Overlay
        if (state.challengeGroupId != null) {
            ChallengeGroupScreen(
                challengeId = state.challengeGroupId!!,
                onBackClick = { viewModel.processIntent(MainIntent.CloseChallengeGroup) },
                onOpenUserChallenge = { ucId ->
                    viewModel.processIntent(MainIntent.CloseChallengeGroup)
                    viewModel.processIntent(MainIntent.OpenChallengeDetail(ucId))
                }
            )
        }

        // Challenge Discover Overlay
        if (state.showChallengeDiscover) {
            ChallengeDiscoverScreen(
                onBackClick = { viewModel.processIntent(MainIntent.CloseChallengeDiscover) },
                onOpenChallengeDetail = { id ->
                    viewModel.processIntent(MainIntent.OpenChallengeDetail(id, isPreview = true))
                },
                onOpenGroupChallenge = { id ->
                    viewModel.processIntent(MainIntent.OpenChallengeGroup(id))
                }
            )
        }

        // Challenge Upcoming List Overlay
        if (state.showChallengeUpcoming) {
            ChallengeUpcomingScreen(
                onBackClick = { viewModel.processIntent(MainIntent.CloseChallengeUpcoming) },
                onOpenChallengeDetail = { id ->
                    viewModel.processIntent(MainIntent.OpenChallengeDetail(id, isPreview = true))
                }
            )
        }

        // Challenge Completed List Overlay
        if (state.showChallengeCompleted) {
            ChallengeCompletedScreen(
                onBackClick = { viewModel.processIntent(MainIntent.CloseChallengeCompleted) },
                onOpenChallengeDetail = { id ->
                    viewModel.processIntent(MainIntent.OpenChallengeDetail(id))
                }
            )
        }

        // Challenge Detail Overlay — kept last so it always renders on top of any
        // list overlay that opened it. See the z-order note above.
        if (state.challengeDetailId != null) {
            val detailId = state.challengeDetailId!!
            ChallengeDetailScreen(
                challengeId = if (state.challengeDetailIsPreview) detailId else null,
                userChallengeId = if (state.challengeDetailIsPreview) null else detailId,
                isPreview = state.challengeDetailIsPreview,
                onBackClick = { viewModel.processIntent(MainIntent.CloseChallengeDetail) },
                onOpenLeaderboard = { cid, title ->
                    viewModel.processIntent(MainIntent.OpenLeaderboard(cid, title))
                }
            )
        }

        // Monthly Leaderboard overlay — full-screen. Mounts above the
        // Challenge Detail overlay so back-stack-navigation feels natural
        // (close leaderboard → back to detail → back to overview).
        if (state.leaderboardChallengeId != null) {
            LeaderboardScreen(
                challengeId = state.leaderboardChallengeId!!,
                challengeTitle = state.leaderboardChallengeTitle,
                onBackClick = { viewModel.processIntent(MainIntent.CloseLeaderboard) }
            )
        }

        // Phase 2B — Global Leaderboard overlay (tabbed).
        if (state.showGlobalLeaderboard) {
            GlobalLeaderboardScreen(
                onBackClick = { viewModel.processIntent(MainIntent.CloseGlobalLeaderboard) }
            )
        }

        // Phase 4 — Verified Share viewer (deep link target + in-app preview).
        state.shareViewerUserId?.let { userId ->
            ShareViewerScreen(
                userId = userId,
                onBackClick = { viewModel.processIntent(MainIntent.CloseShareViewer) }
            )
        }

        // Phase 4 — Share Progress bottom sheet (entry point from Statistics).
        if (state.showShareProgressSheet) {
            ShareProgressBottomSheet(
                onDismiss = { viewModel.processIntent(MainIntent.CloseShareProgressSheet) }
            )
        }

        // Phase 4b — Public profile overlay (deep link betterme://profile/{userId}).
        state.publicProfileUserId?.let { userId ->
            PublicProfileScreen(
                userId = userId,
                onBackClick = { viewModel.processIntent(MainIntent.ClosePublicProfile) }
            )
        }

        // Bottom Nav Bar — ẩn khi overlay đang mở, và cũng ẩn khi đang ở
        // tab ADD (form Tạo thói quen). Lý do: AddHabitScreen hiển thị một
        // sticky CTA "Bắt đầu hành trình" pinned ở Alignment.BottomCenter
        // bằng .navigationBarsPadding(). Nếu BottomNavBar vẫn vẽ (cùng parent
        // Box, z-order sau), thanh điều hướng phủ lên CTA và che hoàn toàn
        // nút bấm. AddHabit có TopBar back arrow để thoát, nên việc ẩn
        // BottomNavBar không làm mất lối điều hướng — đồng nhất với cách
        // mọi overlay khác (CategoryDetail, HabitDetail, ChallengeDiscover...)
        // đã xử lý cùng tình huống.
        val hideBottomBar = state.categoryDetailId != null
            || state.habitDetailId != null
            || state.challengeDetailId != null
            || state.showChallengeDiscover
            || state.showChallengeAchievements
            || state.showChallengeBadges
            || state.challengeGroupId != null
            || state.showChallengeUpcoming
            || state.showChallengeCompleted
            || state.challengeCelebrationId != null
            || state.leaderboardChallengeId != null
            || state.showGlobalLeaderboard
            || state.shareViewerUserId != null
            || state.publicProfileUserId != null
            || state.selectedTab == MainTab.ADD
        if (!hideBottomBar) {
            BottomNavBar(
                selectedTab = state.selectedTab,
                onTabSelected = { viewModel.processIntent(MainIntent.SelectTab(it)) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun PlaceholderTab(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundPrimary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = BetterMeTypography.Headline.Small.SemiBold,
            color = BetterMeColors.Text.TextTertiary
        )
    }
}
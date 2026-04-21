package com.example.betterme.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.betterme.presentation.addhabit.AddHabitScreen
import com.example.betterme.presentation.categorydetail.CategoryDetailIntent
import com.example.betterme.presentation.categorydetail.CategoryDetailScreen
import com.example.betterme.presentation.categorydetail.CategoryDetailViewModel
import com.example.betterme.presentation.dailyhabits.DailyHabitsScreen
import com.example.betterme.presentation.home.HomeScreen
import com.example.betterme.presentation.main.components.BottomNavBar
import com.example.betterme.presentation.main.model.MainTab
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen(
    navigateToSettings: () -> Unit,
    navigateToSignIn: () -> Unit,
    viewModel: MainViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Content area
        when (state.selectedTab) {
            MainTab.HOME -> HomeScreen(
                onLogoutSuccess = navigateToSignIn,
                onCategoryClick = { categoryId, categoryName, categoryIcon ->
                    viewModel.processIntent(
                        MainIntent.OpenCategoryDetail(
                            categoryId = categoryId,
                            categoryName = categoryName,
                            categoryIcon = categoryIcon
                        )
                    )
                }
            )
            MainTab.HABITS -> DailyHabitsScreen()
            MainTab.ADD -> AddHabitScreen(
                onBackClick = { viewModel.processIntent(MainIntent.SelectTab(MainTab.HOME)) }
            )
            MainTab.AI_CHAT -> PlaceholderTab("🤖 AI Chat")
            MainTab.STATS -> PlaceholderTab("📊 Thống kê")
        }

        // Category Detail Overlay
        if (state.categoryDetailId != null) {
            val categoryDetailViewModel = koinViewModel<CategoryDetailViewModel>()
            val detailState by categoryDetailViewModel.viewState.collectAsState()
            val currentCategoryId = state.categoryDetailId

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

            CategoryDetailScreen(
                state = detailState,
                onBack = { viewModel.processIntent(MainIntent.CloseCategoryDetail) },
                onAiReviewClick = {
                    viewModel.processIntent(MainIntent.CloseCategoryDetail)
                    viewModel.processIntent(MainIntent.SelectTab(MainTab.AI_CHAT))
                },
                onAddHabitClick = {
                    viewModel.processIntent(MainIntent.CloseCategoryDetail)
                    viewModel.processIntent(MainIntent.SelectTab(MainTab.ADD))
                },
                onAiSuggestClick = {
                    viewModel.processIntent(MainIntent.CloseCategoryDetail)
                    viewModel.processIntent(MainIntent.SelectTab(MainTab.AI_CHAT))
                }
            )
        }

        // Bottom Nav Bar
        BottomNavBar(
            selectedTab = state.selectedTab,
            onTabSelected = { viewModel.processIntent(MainIntent.SelectTab(it)) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
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
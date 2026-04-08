package com.example.betterme.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.betterme.R
import com.example.betterme.presentation.components.button.BetterMeButton
import com.example.betterme.presentation.onboarding.model.CategoryUiModel
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeShapes
import com.example.betterme.presentation.theme.BetterMeTypography
import org.koin.androidx.compose.koinViewModel

// =========================
// SCREEN
// =========================
@Composable
fun HabitSelectionScreen(
    navigateToSignIn: () -> Unit,
    viewModel: HabitSelectionViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is HabitSelectionEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is HabitSelectionEvent.NavigateNext -> {
                    navigateToSignIn()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        HabitSelectionContent(
            modifier = Modifier.padding(padding),
            state = state,
            onIntent = viewModel::onIntent
        )
    }
}

// =========================
// CONTENT (UI THUẦN)
// =========================
@Composable
fun HabitSelectionContent(
    modifier: Modifier = Modifier,
    state: HabitSelectionState,
    onIntent: (HabitSelectionIntent) -> Unit
) {
    val buttonColor by animateColorAsState(
        targetValue = if (state.selectedCount > 0)
            BetterMeColors.Primary.Primary
        else
            BetterMeColors.Gray.Gray,
        label = ""
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundPrimary)
            .padding(horizontal = 20.dp)
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            text = stringResource(R.string.onboarding_habit_title),
            style = BetterMeTypography.Headline.Small.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            text = stringResource(R.string.onboarding_habit_subtitle),
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextTertiary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.clipToBounds()
        ) {
            items(state.categories) { item ->
                CategoryItem(
                    item = item,
                    onClick = {
                        onIntent(HabitSelectionIntent.ToggleCategory(item.id))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(
                R.string.onboarding_selected_count,
                state.selectedCount
            ),
            style = BetterMeTypography.Body.Medium,
            color = BetterMeColors.Text.TextTertiary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        BetterMeButton(
            onClick = {
                onIntent(HabitSelectionIntent.Continue)
            },
            text = stringResource(R.string.common_continue),
            containerColor = buttonColor,
            modifier = Modifier.heightIn(min = 56.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// =========================
// ITEM
// =========================
@Composable
fun CategoryItem(
    item: CategoryUiModel,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        if (item.isSelected)
            BetterMeColors.Primary.PrimaryBackground
        else
            BetterMeColors.BackGround.BackgroundSecondary,
        label = ""
    )

    val border by animateColorAsState(
        if (item.isSelected)
            BetterMeColors.Primary.Primary
        else
            BetterMeColors.Border.BorderLight,
        label = ""
    )

    val elevation by animateDpAsState(
        targetValue = if (item.isSelected) 6.dp else 0.dp,
        label = ""
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .zIndex(if (item.isSelected) 1f else 0f)
            .clickable { onClick() },
        shape = BetterMeShapes.large,
        colors = CardDefaults.cardColors(containerColor = background),
        border = BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.icon,
                    style = BetterMeTypography.Title.Large.SemiBold
                )
                Text(
                    text = item.name,
                    style = BetterMeTypography.Title.Small.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.description,
                style = BetterMeTypography.Body.Small.SemiBold,
                color = BetterMeColors.Text.TextTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HabitSelectionContentPreview() {

    val fakeState = HabitSelectionState(
        categories = listOf(
            CategoryUiModel(
                id = 1,
                name = "Sức khỏe",
                description = "Tập thể dục, ăn uống lành mạnh",
                icon = "💪",
                isSelected = true
            ),
            CategoryUiModel(
                id = 2,
                name = "Học tập",
                description = "Đọc sách, học kỹ năng mới",
                icon = "📚",
                isSelected = false
            ),
            CategoryUiModel(
                id = 3,
                name = "Thiền",
                description = "Giảm stress, ngủ ngon hơn",
                icon = "🧘",
                isSelected = true
            ),
            CategoryUiModel(
                id = 4,
                name = "Năng suất",
                description = "Quản lý thời gian hiệu quả",
                icon = "⏱",
                isSelected = false
            )
        ),
        selectedCount = 2
    )

    HabitSelectionContent(
        state = fakeState,
        onIntent = {}
    )
}

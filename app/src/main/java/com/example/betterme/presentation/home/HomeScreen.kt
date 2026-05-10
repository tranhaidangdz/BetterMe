package com.example.betterme.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.betterme.presentation.home.components.CantMissCard
import com.example.betterme.presentation.home.components.EditProfileDialog
import com.example.betterme.presentation.home.components.HomeCard
import com.example.betterme.presentation.home.components.HomeProgressCard
import com.example.betterme.presentation.home.components.NotificationBell
import com.example.betterme.presentation.home.components.NotificationCenterSheet
import com.example.betterme.presentation.home.model.CantMiss
import com.example.betterme.presentation.home.model.HomeProgress
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    refreshVersion: Int = 0,
    onCategoryClick: (Int, String, String) -> Unit = { _, _, _ -> },
    onHabitClick: (Int) -> Unit = {},
    onChallengeDetailClick: (Int) -> Unit = {},
    onChallengePreviewClick: (Int) -> Unit = {},
    onViewProgress: () -> Unit = {},
    onLogoutSuccess: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.singleEvent.collectLatest { event ->
            when (event) {
                HomeEvent.NavigateToSignIn -> onLogoutSuccess()
                is HomeEvent.ShowError ->
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT)
                        .show()
                is HomeEvent.OpenChallengeDetail -> onChallengeDetailClick(event.userChallengeId)
                is HomeEvent.OpenChallengePreview -> onChallengePreviewClick(event.challengeId)
            }
        }
    }

    LaunchedEffect(refreshVersion) {
        viewModel.processIntent(HomeIntent.LoadData)
    }

    HomeContent(
        state = state,
        onCategoryClick = onCategoryClick,
        onHabitClick = onHabitClick,
        onViewProgress = onViewProgress,
        onShowEditProfile = { viewModel.processIntent(HomeIntent.ShowEditProfile) },
        onDismissEditProfile = { viewModel.processIntent(HomeIntent.DismissEditProfile) },
        onLogout = { viewModel.processIntent(HomeIntent.Logout) },
        onSaveProfile = { name, photoUri ->
            viewModel.processIntent(HomeIntent.UpdateUserName(name))
            viewModel.processIntent(HomeIntent.UpdateUserPhoto(photoUri))
        },
        onOpenNotificationCenter = { viewModel.processIntent(HomeIntent.OpenNotificationCenter) },
        onDismissNotificationCenter = { viewModel.processIntent(HomeIntent.DismissNotificationCenter) },
        onMarkNotificationRead = { id -> viewModel.processIntent(HomeIntent.MarkNotificationRead(id)) },
        onMarkAllNotificationsRead = { viewModel.processIntent(HomeIntent.MarkAllNotificationsRead) },
        onChallengeNotificationClick = { ucId, challengeId ->
            if (ucId != null) onChallengeDetailClick(ucId)
            else if (challengeId != null) onChallengePreviewClick(challengeId)
        }
    )
}

@Composable
fun HomeContent(
    state: HomeState,
    onCategoryClick: (Int, String, String) -> Unit = { _, _, _ -> },
    onHabitClick: (Int) -> Unit = {},
    onViewProgress: () -> Unit = {},
    onShowEditProfile: () -> Unit = {},
    onDismissEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    onSaveProfile: (String, String) -> Unit = { _, _ -> },
    onOpenNotificationCenter: () -> Unit = {},
    onDismissNotificationCenter: () -> Unit = {},
    onMarkNotificationRead: (Int) -> Unit = {},
    onMarkAllNotificationsRead: () -> Unit = {},
    onChallengeNotificationClick: (Int?, Int?) -> Unit = { _, _ -> }
) {
    val colors = BetterMeColors.ListColors.list

    // Edit Profile Dialog
    if (state.showEditProfileDialog) {
        EditProfileDialog(
            currentName = state.userName,
            currentPhotoUrl = state.userPhotoUrl,
            onDismiss = onDismissEditProfile,
            onSave = onSaveProfile,
            onLogout = onLogout
        )
    }

    // Notification Center
    if (state.showNotificationCenter) {
        NotificationCenterSheet(
            notifications = state.notifications,
            onItemClick = { item ->
                onMarkNotificationRead(item.id)
                onChallengeNotificationClick(item.user_challenge_id, item.challenge_id)
                onDismissNotificationCenter()
            },
            onMarkAllRead = onMarkAllNotificationsRead,
            onDismiss = onDismissNotificationCenter
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundSecondary)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ===== HEADER =====
        item(key = "header") {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar - clickable to edit profile
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BetterMeColors.Primary.PrimaryBackground)
                        .clickable { onShowEditProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.userPhotoUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(state.userPhotoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = BetterMeColors.Primary.Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onShowEditProfile() }
                ) {
                    Text(
                        text = "Hello!",
                        style = BetterMeTypography.Body.Small.Medium,
                        color = BetterMeColors.Text.TextTertiary
                    )
                    Text(
                        text = state.userName.ifBlank { "Guest" },
                        style = BetterMeTypography.Title.Medium.Bold,
                        color = BetterMeColors.Text.TextPrimary
                    )
                }
                NotificationBell(
                    unreadCount = state.unreadNotificationCount,
                    onClick = onOpenNotificationCenter
                )
            }
        }

        // ===== PROGRESS CARD =====
        item(key = "progress") {
            HomeProgressCard(
                progress = state.progress,
                onViewProgress = onViewProgress
            )
        }

        // ===== SECTION: Đang thực hiện =====
        item(key = "cant_miss_header") {
            SectionHeader(title = "Đang thực hiện", count = state.cantMissList.size)
        }

        if (state.cantMissList.isNotEmpty()) {
            item(key = "cant_miss_list") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    itemsIndexed(
                        items = state.cantMissList,
                        key = { _, item -> item.habitId }
                    ) { index, item ->
                        CantMissCard(
                            item = item,
                            cardColor = colors[index % colors.size],
                            onClick = { onHabitClick(item.habitId) }
                        )
                    }
                }
            }
        } else {
            item(key = "cant_miss_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có thói quen nào",
                        style = BetterMeTypography.Body.Medium,
                        color = BetterMeColors.Text.TextTertiary
                    )
                }
            }
        }

        // ===== SECTION: Nhóm thói quen =====
        item(key = "category_header") {
            SectionHeader(title = "Nhóm thói quen", count = state.categoryGroups.size)
        }

        if (state.categoryGroups.isNotEmpty()) {
            itemsIndexed(
                items = state.categoryGroups,
                key = { _, group -> group.categoryId }
            ) { index, group ->
                HomeCard(
                    index = index,
                    group = group,
                    onClick = {
                        onCategoryClick(group.categoryId, group.categoryName, group.categoryIcon)
                    }
                )
            }
        } else {
            item(key = "category_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa chọn nhóm thói quen nào",
                        style = BetterMeTypography.Body.Medium,
                        color = BetterMeColors.Text.TextTertiary
                    )
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(140.dp))
        }
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BetterMeColors.Black.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = BetterMeColors.Primary.Primary)
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // 🔹 Nút xanh (title)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(BetterMeColors.Primary.Primary)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = title,
                style = BetterMeTypography.Title.Medium.Bold,
                color = BetterMeColors.White
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50)) // badge tròn
                .background(BetterMeColors.Primary.Primary.copy(0.5f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$count",
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.White
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    HomeContent(
        state = HomeState(
            userName = "Trần Hải Đăng",
            progress = HomeProgress.fake(),
            cantMissList = CantMiss.fakeList(),
            categoryGroups = listOf(
                HomeCategoryGroup(1, "VẬN ĐỘNG & THỂ CHẤT", "🏃", 10, BetterMeColors.ListColors.list[0]),
                HomeCategoryGroup(2, "DINH DƯỠNG & ĂN UỐNG", "🥗", 14, BetterMeColors.ListColors.list[1]),
                HomeCategoryGroup(3, "TINH THẦN & SỨC KHỎE", "🧠", 7, BetterMeColors.ListColors.list[2]),
            )
        )
    )
}

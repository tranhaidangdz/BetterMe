package com.example.betterme.presentation.challenge.group

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.betterme.R
import com.example.betterme.presentation.challenge.group.components.GroupChallengeHeroCard
import com.example.betterme.presentation.challenge.group.components.LeaderboardRow
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.components.view.SectionHeader
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChallengeGroupScreen(
    challengeId: Int,
    onBackClick: () -> Unit,
    onOpenUserChallenge: (Int) -> Unit,
    viewModel: ChallengeGroupViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(challengeId) {
        viewModel.processIntent(ChallengeGroupIntent.Load(challengeId))
    }

    LaunchedEffect(Unit) {
        viewModel.singleEvent.collectLatest { event ->
            when (event) {
                is ChallengeGroupEvent.Joined -> {
                    Toast.makeText(context, "Đã tham gia thử thách nhóm!", Toast.LENGTH_SHORT).show()
                    onOpenUserChallenge(event.userChallengeId)
                }
                is ChallengeGroupEvent.ShowMessage ->
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundSecondary)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Dark navy top bar
            BetterMeTopBar(
                leadingIconRes = R.drawable.ic_arrow_left,
                title = "Thử thách nhóm",
                onLeadingClick = onBackClick,
                backgroundColor = Color(0xFF1A2540),
                iconTint = Color.White,
                titleColor = Color.White
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    GroupChallengeHeroCard(
                        title = state.title,
                        avatars = state.teams.take(4).map { it.iconEmoji },
                        overflowCount = (state.totalMembers - state.teams.take(4).sumOf { it.memberCount }).coerceAtLeast(0),
                        totalMembers = state.totalMembers,
                        daysCompleted = state.daysCompleted,
                        totalDays = state.totalDays,
                        progressPct = state.progressPct,
                        hasJoined = state.hasJoined,
                        onJoin = { viewModel.processIntent(ChallengeGroupIntent.OpenTeamPicker) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item {
                    SectionHeader(
                        title = "Bảng xếp hạng nhóm",
                        trailingLabel = "Xem tất cả →",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                items(state.teams, key = { it.id }) { team ->
                    LeaderboardRow(
                        model = team,
                        isMyTeam = team.id == state.selectedTeamId,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // Team picker bottom sheet
        if (state.showTeamPicker) {
            TeamPickerSheet(
                teams = state.teams,
                onPick = { viewModel.processIntent(ChallengeGroupIntent.JoinTeam(it)) },
                onDismiss = { viewModel.processIntent(ChallengeGroupIntent.DismissTeamPicker) }
            )
        }
    }
}

@Composable
private fun TeamPickerSheet(
    teams: List<com.example.betterme.presentation.challenge.model.TeamUiModel>,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(BetterMeColors.White)
                .navigationBarsPadding()
                .padding(20.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* swallow click */ }
        ) {
            Text(
                text = "Chọn đội",
                style = BetterMeTypography.Title.Medium.Bold,
                color = BetterMeColors.Text.TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            teams.forEach { team ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(team.accentColor.copy(alpha = 0.1f))
                        .clickable { onPick(team.id) }
                        .padding(16.dp)
                ) {
                    Text(
                        text = "${team.iconEmoji} ${team.name}  ·  ${team.memberCount} thành viên",
                        style = BetterMeTypography.Body.Medium,
                        color = BetterMeColors.Text.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

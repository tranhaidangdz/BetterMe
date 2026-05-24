package com.example.betterme.presentation.challenge.detail

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.betterme.R
import com.example.betterme.presentation.challenge.celebration.components.CompletionCelebrationDialog
import com.example.betterme.presentation.challenge.celebration.components.SharePlatform
import com.example.betterme.presentation.challenge.detail.components.ChallengeHeroCard
import com.example.betterme.presentation.challenge.detail.components.ChallengeStatsRow
import com.example.betterme.presentation.challenge.detail.components.ContinueChallengeBottomBar
import com.example.betterme.presentation.challenge.detail.components.DescriptionBulletList
import com.example.betterme.presentation.challenge.detail.components.FailedChallengeBanner
import com.example.betterme.presentation.challenge.detail.components.RewardRow
import com.example.betterme.presentation.challenge.detail.components.WeekStreakRow
import com.example.betterme.presentation.components.checkin.CheckInConfirmSheet
import com.example.betterme.presentation.components.checkin.CheckInSuccessSheet
import com.example.betterme.presentation.components.checkin.CheckInUiState
import com.example.betterme.presentation.components.view.BetterMeTopBar
import com.example.betterme.presentation.leaderboard.LeaderboardIntent
import com.example.betterme.presentation.leaderboard.LeaderboardUi
import com.example.betterme.presentation.leaderboard.LeaderboardViewModel
import com.example.betterme.presentation.leaderboard.components.LeaderboardSummaryCard
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import com.example.betterme.utils.ChallengeShareImagePrep
import com.example.betterme.utils.ShareUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.util.Locale

/**
 * Challenge Detail screen — supports PREVIEW (not yet joined), ACTIVE (in progress) and
 * COMPLETED modes. Camera-based check-in mirrors the habit flow via the shared
 * [CheckInConfirmSheet] / [CheckInSuccessSheet].
 *
 * The completion celebration dialog (Screen 9) is shown by MainScreen when
 * `state.celebration != null` — this screen emits the state but does not own the dialog.
 */
@Composable
fun ChallengeDetailScreen(
    challengeId: Int? = null,
    userChallengeId: Int? = null,
    isPreview: Boolean = false,
    onBackClick: () -> Unit,
    onOpenLeaderboard: (Int, String) -> Unit = { _, _ -> },
    viewModel: ChallengeDetailViewModel = koinViewModel(),
    leaderboardViewModel: LeaderboardViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()
    val leaderboardState by leaderboardViewModel.viewState.collectAsState()
    val context = LocalContext.current

    // Initialize the leaderboard preview once the challenge id + title
    // are known. Uses the session-cached read path so re-entering the
    // detail screen for the same challenge is instant.
    LaunchedEffect(state.challengeId, state.title) {
        if (state.challengeId != 0 && state.title.isNotBlank()) {
            leaderboardViewModel.processIntent(
                LeaderboardIntent.Initialize(state.challengeId, state.title)
            )
        }
    }

    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            viewModel.processIntent(ChallengeDetailIntent.PhotoCaptured(photoUri!!))
        } else {
            // User cancelled or took no picture — close the confirm sheet.
            viewModel.processIntent(ChallengeDetailIntent.DismissCheckIn)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (cameraGranted) {
            if (locationGranted) {
                fetchLocation(context, viewModel)
            }
            val uri = createImageUri(context)
            photoUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Cần quyền camera để check-in", Toast.LENGTH_SHORT).show()
            viewModel.processIntent(ChallengeDetailIntent.DismissCheckIn)
        }
    }

    // The ChallengeDetailViewModel is scoped to MainScreen's parent ViewModelStoreOwner,
    // so the same VM instance is reused every time this overlay opens. Without an
    // explicit reset, switching from challenge A to challenge B would render A's title /
    // hero / strip for one frame before B's load coroutine finishes — the "stacked
    // overlay" stale-flash users reported. Two layered guards eliminate that:
    //
    // 1. DisposableEffect.onDispose wipes state synchronously when the overlay leaves
    //    composition (back tap → state.challengeDetailId = null → screen unmounts).
    //    The next mount starts from a clean ChallengeDetailState() with isLoading=true,
    //    which the existing loading overlay already covers — no visible stale data.
    //
    // 2. The load LaunchedEffect dispatches Reset before LoadPreview/LoadActive too,
    //    catching the rare A→B direct switch where the overlay never unmounts (e.g., a
    //    list row tap inside a context where the detail is already open). The reset
    //    fires synchronously inside processIntent before the suspending DB read kicks
    //    off, so the recomposition triggered by LaunchedEffect's first state write sees
    //    a clean slate, not the previous challenge's data.
    DisposableEffect(Unit) {
        onDispose { viewModel.processIntent(ChallengeDetailIntent.Reset) }
    }

    LaunchedEffect(challengeId, userChallengeId, isPreview) {
        viewModel.processIntent(ChallengeDetailIntent.Reset)
        when {
            userChallengeId != null && !isPreview ->
                viewModel.processIntent(ChallengeDetailIntent.LoadActive(userChallengeId))
            challengeId != null ->
                viewModel.processIntent(ChallengeDetailIntent.LoadPreview(challengeId))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.singleEvent.collectLatest { event ->
            when (event) {
                is ChallengeDetailEvent.ShowMessage ->
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                ChallengeDetailEvent.LaunchCamera -> {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
                ChallengeDetailEvent.FetchLocation -> {
                    // Fired alongside LaunchCamera; permission flow handles the actual call.
                }
                is ChallengeDetailEvent.LaunchShareSheet -> {
                    // Stage + compress images on the IO dispatcher so the share
                    // sheet doesn't open with a freeze; missing/unreachable
                    // sources are silently dropped by the prep helper.
                    val prep = ChallengeShareImagePrep(context.applicationContext)
                    val uris = prep.prepare(event.imageSources)
                    ShareUtils.shareChallengeWithImages(
                        context = context,
                        message = event.message,
                        imageUris = uris
                    )
                }
                ChallengeDetailEvent.NavigateBack -> onBackClick()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BetterMeColors.BackGround.BackgroundSecondary)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    BetterMeTopBar(
                        leadingIconRes = R.drawable.ic_arrow_left,
                        title = "Chi tiết thử thách",
                        onLeadingClick = onBackClick,
                        trailingIconRes = R.drawable.ic_share,
                        onTrailingClick = { viewModel.processIntent(ChallengeDetailIntent.Share) }
                    )
                }

                item {
                    ChallengeHeroCard(
                        title = state.title,
                        // Prefer the curated short description; fall back to the long one
                        // so legacy challenges without short_description still render.
                        subtitle = state.shortDescription.ifBlank { state.description },
                        iconEmoji = state.iconEmoji,
                        accentColor = state.accentColor,
                        difficulty = state.difficulty,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (state.motivationalQuote.isNotBlank()) {
                    item {
                        Text(
                            text = "“${state.motivationalQuote}”",
                            style = BetterMeTypography.Body.Small.Medium,
                            color = state.accentColor,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }

                item {
                    val completedDays = if (state.mode == DetailMode.Preview) 0 else state.currentStreak
                    val totalDays = if (state.targetStreak > 0) state.targetStreak else state.durationDays
                    val pct = if (state.mode == DetailMode.Preview) 0 else state.progressPct
                    val remaining = if (state.mode == DetailMode.Preview) totalDays else state.daysRemaining
                    ChallengeStatsRow(
                        completedDays = completedDays,
                        totalDays = totalDays,
                        completionPct = pct,
                        daysRemaining = remaining,
                        accentColor = state.accentColor,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (state.mode == DetailMode.Failed) {
                    item {
                        FailedChallengeBanner(
                            failedAtDate = state.failedAtDate,
                            targetEndDate = state.targetEndDate,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                if (state.mode == DetailMode.Active ||
                    state.mode == DetailMode.Completed ||
                    state.mode == DetailMode.Failed
                ) {
                    item {
                        Text(
                            text = "Lịch sử",
                            style = BetterMeTypography.Title.Small.Bold,
                            color = BetterMeColors.Text.TextPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item {
                        WeekStreakRow(
                            days = state.weekStrip,
                            accentColor = state.accentColor,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                item {
                    Text(
                        text = "Phần thưởng",
                        style = BetterMeTypography.Title.Small.Bold,
                        color = BetterMeColors.Text.TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item {
                    RewardRow(
                        coins = state.rewardCoins,
                        badgeName = state.rewardBadgeName,
                        badgeImageUrl = state.rewardBadgeImageUrl,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item {
                    Text(
                        text = "Mô tả thử thách",
                        style = BetterMeTypography.Title.Small.Bold,
                        color = BetterMeColors.Text.TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                if (state.description.isNotBlank()) {
                    item {
                        Text(
                            text = state.description,
                            style = BetterMeTypography.Body.Medium,
                            color = BetterMeColors.Text.TextSecondary,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
                if (state.descriptionBullets.isNotEmpty()) {
                    item {
                        DescriptionBulletList(
                            bullets = state.descriptionBullets,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Monthly leaderboard preview — renders only when the
                // session-cached snapshot has resolved with entries. The
                // card itself handles the empty case (returns no UI).
                (leaderboardState.ui as? LeaderboardUi.Success)?.let { ui ->
                    item {
                        Text(
                            text = "Bảng xếp hạng tháng",
                            style = BetterMeTypography.Title.Small.Bold,
                            color = BetterMeColors.Text.TextPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item {
                        LeaderboardSummaryCard(
                            snapshot = ui.snapshot,
                            onOpenFullLeaderboard = {
                                onOpenLeaderboard(state.challengeId, state.title)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            // Sticky bottom CTA. Active mode disables the button when today's check-in
            // is already done so the streak cannot be double-counted.
            when (state.mode) {
                DetailMode.Preview -> {
                    val (joinLabel, joinEnabled) = when {
                        state.isJoining -> "Đang tham gia..." to false
                        state.isLoading -> "Tham gia thử thách" to false
                        else -> "Tham gia thử thách" to true
                    }
                    ContinueChallengeBottomBar(
                        label = joinLabel,
                        enabled = joinEnabled,
                        onClick = { viewModel.processIntent(ChallengeDetailIntent.JoinChallenge) }
                    )
                }
                DetailMode.Active -> {
                    val alreadyCheckedInToday = state.weekStrip.any {
                        it.isToday && it.status == com.example.betterme.presentation.challenge.model.DayStatus.Done
                    }
                    val (label, enabled) = when {
                        state.isSavingCheckIn -> "Đang lưu..." to false
                        alreadyCheckedInToday -> "Hôm nay đã check-in" to false
                        else -> "Check-in hôm nay" to true
                    }
                    ContinueChallengeBottomBar(
                        label = label,
                        enabled = enabled,
                        onClick = { viewModel.processIntent(ChallengeDetailIntent.StartCheckIn) }
                    )
                }
                DetailMode.Failed -> {
                    // History-only check-ins. The button keeps a softer label so the
                    // user understands the action doesn't recover the challenge.
                    val alreadyCheckedInToday = state.weekStrip.any {
                        it.isToday && it.status == com.example.betterme.presentation.challenge.model.DayStatus.Done
                    }
                    val (label, enabled) = when {
                        state.isSavingCheckIn -> "Đang lưu..." to false
                        alreadyCheckedInToday -> "Hôm nay đã check-in" to false
                        else -> "Ghi check-in lịch sử" to true
                    }
                    ContinueChallengeBottomBar(
                        label = label,
                        enabled = enabled,
                        onClick = { viewModel.processIntent(ChallengeDetailIntent.StartCheckIn) }
                    )
                }
                DetailMode.Completed -> Unit
            }
        }

        if (state.checkInStep == CheckInStep.Confirm) {
            CheckInConfirmSheet(
                state = CheckInUiState(
                    photoUri = state.checkInPhotoUri,
                    note = state.checkInNote,
                    timestamp = state.checkInTimestamp,
                    latitude = state.checkInLatitude,
                    longitude = state.checkInLongitude,
                    locationName = state.checkInLocationName,
                    isSaving = state.isSavingCheckIn
                ),
                onNoteChanged = { viewModel.processIntent(ChallengeDetailIntent.UpdateNote(it)) },
                onConfirm = { viewModel.processIntent(ChallengeDetailIntent.ConfirmCheckIn) },
                onDismiss = { viewModel.processIntent(ChallengeDetailIntent.DismissCheckIn) }
            )
        }

        if (state.checkInStep == CheckInStep.Success) {
            CheckInSuccessSheet(
                entityTitle = state.title,
                currentStreak = state.currentStreak,
                onDismiss = { viewModel.processIntent(ChallengeDetailIntent.DismissSuccess) },
                onViewHistory = { viewModel.processIntent(ChallengeDetailIntent.DismissSuccess) }
            )
        }

        val celebration = state.celebration
        if (celebration != null) {
            CompletionCelebrationDialog(
                challengeTitle = celebration.challengeTitle,
                coinsEarned = celebration.coinsEarned,
                badgeName = celebration.badgeName,
                difficultyRaw = state.difficulty.raw,
                durationDays = state.durationDays,
                completionMessage = state.completionMessage,
                onShare = { _: SharePlatform ->
                    viewModel.processIntent(ChallengeDetailIntent.Share)
                },
                onDismiss = { viewModel.processIntent(ChallengeDetailIntent.DismissCelebration) }
            )
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BetterMeColors.Black.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = BetterMeColors.Primary.Primary,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

// ===== Helpers (mirroring HabitDetailScreen) =====

private fun createImageUri(context: Context): Uri {
    val imageDir = File(context.cacheDir, "checkin_images").apply { mkdirs() }
    val imageFile = File(imageDir, "challenge_checkin_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

@Suppress("MissingPermission")
private fun fetchLocation(context: Context, viewModel: ChallengeDetailViewModel) {
    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val token = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val name = try {
                        val geo = Geocoder(context, Locale.forLanguageTag("vi"))
                        @Suppress("DEPRECATION")
                        val addrs = geo.getFromLocation(location.latitude, location.longitude, 1)
                        addrs?.firstOrNull()?.let { addr ->
                            buildString {
                                addr.thoroughfare?.let { append(it) }
                                addr.subAdminArea?.let {
                                    if (isNotEmpty()) append(", ")
                                    append(it)
                                }
                                addr.adminArea?.let {
                                    if (isNotEmpty()) append(", ")
                                    append(it)
                                }
                            }.ifBlank { null }
                        }
                    } catch (_: Exception) { null }
                    viewModel.processIntent(
                        ChallengeDetailIntent.SetLocation(location.latitude, location.longitude, name)
                    )
                }
            }
    } catch (_: Exception) { }
}

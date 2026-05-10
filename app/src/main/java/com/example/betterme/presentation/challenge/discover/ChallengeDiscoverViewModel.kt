package com.example.betterme.presentation.challenge.discover

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.room.entities.ChallengeEntity
import com.example.betterme.domain.repository.CategoryRepository
import com.example.betterme.domain.repository.ChallengeRepository
import com.example.betterme.presentation.challenge.model.CategoryTileUi
import com.example.betterme.presentation.challenge.model.FeaturedChallengeUiModel
import com.example.betterme.presentation.challenge.model.NewChallengeUiModel
import com.example.betterme.presentation.challenge.shared.Difficulty
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChallengeDiscoverViewModel(
    private val challengeRepository: ChallengeRepository,
    private val categoryRepository: CategoryRepository
) : BaseMviViewModel<ChallengeDiscoverIntent, ChallengeDiscoverState, ChallengeDiscoverEvent>() {

    override fun initState() = ChallengeDiscoverState()

    init {
        processIntent(ChallengeDiscoverIntent.Load)
    }

    override fun processIntent(intent: ChallengeDiscoverIntent) {
        when (intent) {
            ChallengeDiscoverIntent.Load -> load()
            is ChallengeDiscoverIntent.UpdateQuery -> {
                updateState { copy(query = intent.q) }
                runSearch(intent.q)
            }
            is ChallengeDiscoverIntent.SelectCategory -> updateState { copy(selectedCategoryId = intent.id) }
            is ChallengeDiscoverIntent.SelectDifficulty -> updateState { copy(selectedDifficulty = intent.difficulty) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            combine(
                challengeRepository.observeAll(),
                categoryRepository.getAll()
            ) { challenges, categories -> challenges to categories }.collect { (challenges, categories) ->
                val byCategory = challenges.groupBy { it.category_id }
                val tiles = categories.map { cat ->
                    CategoryTileUi(
                        categoryId = cat.id,
                        name = cat.name,
                        emoji = cat.icon,
                        challengeCount = byCategory[cat.id]?.size ?: 0,
                        accentColor = colorForCategory(cat.id)
                    )
                }
                val now = System.currentTimeMillis()
                // Featured = is_featured AND not in the future (those go in upcomingFeatured).
                val featured = challenges.filter {
                    it.is_featured && (it.start_date == null || it.start_date <= now)
                }.map { it.toFeaturedUi() }
                // Upcoming = future start_date, sorted by soonest first, capped at 10
                // for the carousel.
                val upcomingFeatured = challenges
                    .filter { (it.start_date ?: 0L) > now }
                    .sortedBy { it.start_date }
                    .take(10)
                    .map { it.toUpcomingUi(now) }
                val newest = challenges
                    .filter { (it.start_date ?: 0L) <= now }
                    .sortedByDescending { it.created_at }
                    .take(10)
                    .map { it.toNewUi() }
                updateState {
                    copy(
                        isLoading = false,
                        featured = featured,
                        upcomingFeatured = upcomingFeatured,
                        categories = tiles,
                        newest = newest
                    )
                }
            }
        }
    }

    private fun runSearch(q: String) {
        if (q.isBlank()) {
            updateState { copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            val results = challengeRepository.search(q).map { it.toNewUi() }
            updateState { copy(searchResults = results) }
        }
    }

    private fun ChallengeEntity.toUpcomingUi(now: Long): com.example.betterme.presentation.challenge.model.UpcomingFeatureUiModel {
        val DAY_MS = 24L * 60L * 60L * 1000L
        val days = ((((start_date ?: now) - now) / DAY_MS).toInt()).coerceAtLeast(0)
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("vi"))
        return com.example.betterme.presentation.challenge.model.UpcomingFeatureUiModel(
            challengeId = id,
            title = title,
            iconEmoji = icon_emoji,
            accentColor = parseColor(color_hex),
            daysUntilStart = days,
            startLabel = sdf.format(java.util.Date(start_date ?: now)),
            rewardCoins = reward_coins,
            participantCount = participant_count
        )
    }

    private fun ChallengeEntity.toFeaturedUi() = FeaturedChallengeUiModel(
        challengeId = id,
        title = title,
        durationDaysLabel = "$duration_days ngày liên tiếp",
        iconEmoji = icon_emoji,
        accentColor = parseColor(color_hex),
        rewardCoins = reward_coins,
        participantCount = participant_count,
        isGroup = is_group
    )

    private fun ChallengeEntity.toNewUi() = NewChallengeUiModel(
        challengeId = id,
        title = title,
        iconEmoji = icon_emoji,
        accentColor = parseColor(color_hex),
        difficulty = Difficulty.fromRaw(difficulty),
        rewardCoins = reward_coins,
        participantCount = participant_count,
        isGroup = is_group
    )

    private fun parseColor(hex: String): Color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF0077FF)
    }

    private fun colorForCategory(id: Int): Color = listOf(
        Color(0xFF3B82F6),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899),
        Color(0xFF06B6D4)
    )[(id - 1).coerceAtLeast(0) % 6]
}

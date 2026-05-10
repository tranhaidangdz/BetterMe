package com.example.betterme.presentation.challenge.badges

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.example.betterme.base.BaseMviViewModel
import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.data.local.room.entities.AchievementEntity
import com.example.betterme.domain.repository.AchievementRepository
import com.example.betterme.domain.repository.UserAchievementRepository
import com.example.betterme.presentation.challenge.model.BadgeUiModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChallengeBadgesViewModel(
    private val dataStoreManager: DataStoreManager,
    private val achievementRepository: AchievementRepository,
    private val userAchievementRepository: UserAchievementRepository
) : BaseMviViewModel<ChallengeBadgesIntent, ChallengeBadgesState, ChallengeBadgesEvent>() {

    override fun initState() = ChallengeBadgesState()

    init {
        processIntent(ChallengeBadgesIntent.Load)
    }

    override fun processIntent(intent: ChallengeBadgesIntent) {
        when (intent) {
            ChallengeBadgesIntent.Load -> load()
            is ChallengeBadgesIntent.SetFilter -> updateState { copy(filter = intent.filter) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
            val earnedFlow = if (userId.isBlank()) flowOf(emptyList())
            else userAchievementRepository.observeByUser(userId)

            combine(achievementRepository.observeAll(), earnedFlow) { all, earned ->
                val earnedById = earned.associateBy { it.achievement_id }
                // Section titles match the numbered headings in the design mockup,
                // plus the Discipline + Mental Wellness categories introduced for the
                // 6-bucket curated content expansion.
                val sectionOrder = listOf(
                    "BASIC" to "1. Huy hiệu cơ bản",
                    "HEALTH" to "2. Huy hiệu sức khỏe",
                    "LEARNING" to "3. Huy hiệu học tập",
                    "MENTAL_WELLNESS" to "4. Huy hiệu sức khỏe tinh thần",
                    "DISCIPLINE" to "5. Huy hiệu kỷ luật",
                    "SPECIAL" to "6. Huy hiệu đặc biệt"
                )
                val builtSections = sectionOrder.map { (key, label) ->
                    val list = all.filter { it.category == key }
                        .sortedBy { it.sort_order }
                        .map { it.toUi(earnedById[it.id]?.achieved_at) }
                    BadgeSectionUi(title = label, badges = list)
                }
                Triple(builtSections, all.size, earnedById.size)
            }.collect { (sections, total, earnedCount) ->
                updateState {
                    copy(
                        isLoading = false,
                        sections = sections,
                        totalBadges = total,
                        earnedBadges = earnedCount
                    )
                }
            }
        }
    }

    private fun AchievementEntity.toUi(earnedAt: Long?): BadgeUiModel {
        return BadgeUiModel(
            id = id,
            name = title,
            description = description,
            iconRes = icon,
            iconEmoji = if (icon_emoji.isNotBlank()) icon_emoji else "🏅",
            imageUrl = image_url,
            accentColor = parseColor(color_hex),
            isEarned = earnedAt != null,
            earnedAtLabel = earnedAt?.let {
                SimpleDateFormat("dd/MM/yyyy", Locale("vi")).format(Date(it))
            }
        )
    }

    private fun parseColor(hex: String): Color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF0077FF)
    }
}

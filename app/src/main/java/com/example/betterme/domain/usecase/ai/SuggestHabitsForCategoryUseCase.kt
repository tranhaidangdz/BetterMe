package com.example.betterme.domain.usecase.ai

import com.example.betterme.data.local.datastore.DataStoreManager
import com.example.betterme.domain.ai.AiCoachPersonality
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.ai.AiHabitInsightRepository.AiSuggestResult
import com.example.betterme.domain.repository.HabitRepository
import kotlinx.coroutines.flow.first

/**
 * Generates AI habit suggestions for a category. Pulls the user's existing habit
 * titles in that category so the AI doesn't suggest duplicates, then delegates to
 * the AI repo which handles structured JSON parsing.
 */
class SuggestHabitsForCategoryUseCase(
    private val dataStoreManager: DataStoreManager,
    private val habitRepository: HabitRepository,
    private val aiRepository: AiHabitInsightRepository
) {
    suspend operator fun invoke(
        categoryId: Int,
        categoryName: String,
        personality: AiCoachPersonality = AiCoachPersonality.Default
    ): AiSuggestResult {
        val userId = dataStoreManager.getCurrentUserId().first().orEmpty()
        if (userId.isBlank()) {
            return AiSuggestResult.Failure("Vui lòng đăng nhập để dùng AI")
        }
        val existing = habitRepository.getHabits(userId).first()
            .filter { it.category_id == categoryId }
            .map { it.title }
        return aiRepository.suggestHabits(
            categoryName = categoryName,
            existingHabitTitles = existing,
            personality = personality
        )
    }
}

package com.example.betterme.data.ai

import android.util.Log
import com.example.betterme.data.ai.dto.ChatMessage
import com.example.betterme.data.ai.dto.ChatRequest
import com.example.betterme.domain.ai.AiCoachPersonality
import com.example.betterme.domain.ai.AiHabitInsightRepository
import com.example.betterme.domain.ai.AiHabitInsightRepository.AiResult
import com.example.betterme.domain.ai.AiHabitInsightRepository.AiSuggestResult
import com.example.betterme.domain.ai.ScheduleHabitInput
import com.example.betterme.domain.ai.SuggestedHabit
import com.example.betterme.domain.ai.habitcreation.CreationRiskLevel
import com.example.betterme.domain.ai.habitcreation.CreationSuggestionType
import com.example.betterme.domain.ai.habitcreation.HabitCreationAnalysis
import com.example.betterme.domain.ai.habitcreation.HabitCreationInput
import com.example.betterme.domain.ai.habitcreation.HabitCreationSuggestion
import com.example.betterme.domain.ai.habitcreation.HabitCreationWarning
import com.example.betterme.domain.ai.habitcreation.WarningType
import com.example.betterme.domain.ai.lifestyle.AdaptiveSuggestion
import com.example.betterme.domain.ai.lifestyle.EnergyPattern
import com.example.betterme.domain.ai.lifestyle.HabitCompletionRecord
import com.example.betterme.domain.ai.lifestyle.LifestyleInsight
import com.example.betterme.domain.ai.lifestyle.OverallTrend
import com.example.betterme.domain.ai.lifestyle.SuggestionType
import com.example.betterme.domain.ai.progression.HabitProgressionAction
import com.example.betterme.domain.ai.progression.HabitProgressionAnalysis
import com.example.betterme.domain.ai.progression.HabitProgressionInput
import com.example.betterme.domain.ai.progression.ProgressionActionType
import com.example.betterme.domain.ai.progression.ProgressionPace
import com.example.betterme.domain.ai.progression.ProgressionTrigger
import com.example.betterme.domain.ai.progression.VibrantHabit
import com.example.betterme.domain.ai.recovery.HabitRecoveryAction
import com.example.betterme.domain.ai.recovery.HabitRecoveryAnalysis
import com.example.betterme.domain.ai.recovery.HabitRecoveryInput
import com.example.betterme.domain.ai.recovery.RecoveryActionType
import com.example.betterme.domain.ai.recovery.RecoveryIntensity
import com.example.betterme.domain.ai.recovery.RecoveryTrigger
import com.example.betterme.domain.ai.recovery.StrugglingHabit
import com.example.betterme.domain.ai.onboarding.Difficulty
import com.example.betterme.domain.ai.onboarding.HabitCategoryKey
import com.example.betterme.domain.ai.onboarding.OnboardingProfile
import com.example.betterme.domain.ai.onboarding.OnboardingSuggestedHabit
import com.example.betterme.domain.ai.onboarding.OnboardingSuggestion
import com.example.betterme.domain.ai.onboarding.Priority
import com.example.betterme.domain.ai.schedule.BurnoutRisk
import com.example.betterme.domain.ai.schedule.ConflictType
import com.example.betterme.domain.ai.schedule.EnergyLevel
import com.example.betterme.domain.ai.schedule.HealthyDefaults
import com.example.betterme.domain.ai.schedule.OptimizedHabitTime
import com.example.betterme.domain.ai.schedule.ScheduleAnalysis
import com.example.betterme.domain.ai.schedule.ScheduleConflict
import com.example.betterme.domain.ai.schedule.UserLifestyleProfile
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * OpenRouter-backed implementation.
 *
 * Model chain (tried in order)
 * 1. google/gemini-2.5-flash-preview:free  — fastest free model, strongest JSON.
 * 2. google/gemini-2.0-flash-exp:free      — previous-gen Gemini, separate quota.
 * 3. meta-llama/llama-3.3-70b-instruct:free — strong reasoning, slower.
 * 4. mistralai/mistral-small-3.1-24b-instruct:free — last-line free option.
 *
 * Per-model behaviour
 * - One retry on 429/500/502/503/504 with 800ms backoff. Anything else fails
 *   fast — retrying 401/403/404 just burns time.
 * - On any failure, the chain advances to the next model. The user never sees
 *   intermediate failures unless EVERY model is exhausted.
 *
 * Last-resort fallback
 * - If all 4 models fail, the repo returns a canned (handwritten) response
 *   tagged with `isCanned = true`. The use cases never cache canned content,
 *   so the next attempt is fresh. The UI renders canned content through the
 *   same premium card — the user always sees meaningful content.
 *
 * Output shaping
 * - max_tokens = 120 across the board. Prompts are deliberately terse so a
 *   120-token reply still feels complete. This trades occasional truncation
 *   (caught by the canned fallback) for a much smaller per-call quota burn.
 */
class AiHabitInsightRepositoryImpl(
    private val api: OpenRouterApi
) : AiHabitInsightRepository {

    // ============================================================
    // REVIEW
    // ============================================================
    override suspend fun reviewHabitGroup(
        categoryName: String,
        stats: String,
        personality: AiCoachPersonality
    ): AiResult {
        val systemPrompt = personality.systemPromptPrefix +
            "\nTrả lời tiếng Việt, 3-4 câu, súc tích, chỉ dùng số có trong dữ liệu."
        val userPrompt = "Nhóm: \"$categoryName\".\n$stats"

        val messages = listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = userPrompt)
        )

        var lastFailure: AiResult.Failure? = null
        for ((index, model) in FALLBACK_MODELS.withIndex()) {
            val result = retryOnTransient { tryModel(model, messages) }
            if (result is AiResult.Success) return result
            lastFailure = result as AiResult.Failure
            Log.w(TAG, "Review model[$index]=$model failed: ${lastFailure.message}")
        }

        Log.w(TAG, "All review models exhausted — serving canned review")
        return AiResult.Success(text = cannedReview(categoryName), isCanned = true)
    }

    // ============================================================
    // SUGGESTIONS
    // ============================================================
    override suspend fun suggestHabits(
        categoryName: String,
        existingHabitTitles: List<String>,
        personality: AiCoachPersonality
    ): AiSuggestResult {
        val existingList = if (existingHabitTitles.isEmpty()) "không có"
        else existingHabitTitles.joinToString("; ")

        // Compact JSON-only prompt. No markdown rules, no "BẮT BUỘC" filler — the
        // model is told the exact schema once and asked to fill it.
        val systemPrompt =
            """
            Trả về JSON: {"suggestions":[{"title":"","emoji":"","description":"","difficulty":"EASY|MEDIUM|HARD","estimatedImpact":"","streakBenefit":""}]}
            3-4 mục, tiếng Việt rất ngắn, không trùng thói quen đã có. Không kèm chữ ngoài JSON.
            """.trimIndent()
        val userPrompt = "Nhóm: \"$categoryName\". Đã có: $existingList."

        val messages = listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = userPrompt)
        )

        var lastFailure: AiSuggestResult.Failure? = null
        for ((index, model) in FALLBACK_MODELS.withIndex()) {
            val result = retryOnTransient { tryModelJson(model, messages) }
            if (result is AiSuggestResult.Success) return result
            lastFailure = result as AiSuggestResult.Failure
            Log.w(TAG, "Suggest model[$index]=$model failed: ${lastFailure.message}")
        }

        Log.w(TAG, "All suggest models exhausted — serving canned suggestions")
        return AiSuggestResult.Success(suggestions = cannedSuggestions(), isCanned = true)
    }

    // ============================================================
    // SCHEDULE CONFLICT ANALYZER
    // ============================================================
    override suspend fun analyzeSchedule(
        profile: UserLifestyleProfile?,
        habits: List<ScheduleHabitInput>
    ): ScheduleAnalysis {
        val withReminders = habits.filter { it.reminderTime.matches(HHMM_REGEX) }
        if (withReminders.size < 2) {
            // Match the prompt's empty-data contract. No model call needed.
            return emptyDataAnalysis()
        }

        val effectiveProfile = profile ?: UserLifestyleProfile.Default
        val systemPrompt = SCHEDULE_SYSTEM_PROMPT
        val userPrompt = buildScheduleUserPrompt(withReminders, effectiveProfile)
        val messages = listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = userPrompt)
        )

        var lastFailure: String? = null
        for ((index, model) in FALLBACK_MODELS.withIndex()) {
            // No retryOnTransient wrapper here: the existing `isTransientFailure`
            // signature is tied to AiResult/AiSuggestResult sealed types and would
            // be a no-op on Result<ScheduleAnalysis>. The 4-model chain itself
            // already provides redundancy for transient 429/5xx — when one model
            // throttles, the next probably has fresh quota.
            val attempt = tryScheduleModel(model, messages)
            attempt.onSuccess { return it }
            lastFailure = attempt.exceptionOrNull()?.message
            Log.w(TAG, "Schedule model[$index]=$model failed: $lastFailure")
        }

        Log.w(TAG, "All schedule models exhausted — serving canned analysis")
        return cannedScheduleAnalysis(withReminders, effectiveProfile)
    }

    private suspend fun tryScheduleModel(
        model: String,
        messages: List<ChatMessage>
    ): Result<ScheduleAnalysis> {
        Log.d(TAG, "Using model=$model")
        return try {
            val response = api.chatCompletion(
                ChatRequest(
                    model = model,
                    messages = messages,
                    // 300 tokens fits the bounded output: 3 conflicts × ~30 tokens
                    // + 5 optimizations × ~10 tokens + summary + positiveFeedback +
                    // top-level enums + scores. Headroom for occasional Vietnamese
                    // multi-byte expansion.
                    maxTokens = SCHEDULE_MAX_TOKENS,
                    temperature = TEMPERATURE
                )
            )
            if (response.error != null) {
                return Result.failure(
                    IllegalStateException(response.error.message ?: "AI từ chối yêu cầu")
                )
            }
            val content = response.choices.firstOrNull()?.message?.content?.trim()
            if (content.isNullOrBlank()) {
                return Result.failure(IllegalStateException("AI không trả lời"))
            }
            val cleaned = content
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = parseScheduleAnalysis(cleaned)
                ?: return Result.failure(IllegalStateException("AI trả về dữ liệu sai định dạng"))
            Result.success(parsed)
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(IllegalStateException("Mạng chậm (504)"))
        } catch (e: HttpException) {
            // Reuse the existing HTTP error mapper for friendly Vietnamese reasons.
            // Wrapped as IllegalStateException so the retry path sees it as a
            // transient signal when the code is 429/5xx and a fatal signal otherwise.
            Result.failure(IllegalStateException(extractHttpErrorMessage(e)))
        } catch (e: java.io.IOException) {
            Result.failure(IllegalStateException("Không thể kết nối đến AI"))
        } catch (e: Exception) {
            Log.e(TAG, "Schedule request threw", e)
            Result.failure(e)
        }
    }

    private fun buildScheduleUserPrompt(
        habits: List<ScheduleHabitInput>,
        profile: UserLifestyleProfile
    ): String = buildString {
        appendLine("Habits:")
        habits.forEach { h ->
            appendLine(
                "- \"${h.title}\" — ${h.reminderTime}, ${h.difficulty} difficulty, " +
                    "${h.priority} priority, ${h.estimatedMinutes} min"
            )
        }
        appendLine()
        appendLine("Sleep: ${profile.sleepStart} → ${profile.sleepEnd} (target ${profile.sleepDurationTargetHours}h)")
        appendLine("Work: ${profile.workStart} → ${profile.workEnd}")
        appendLine("Meals: breakfast ${profile.breakfast}, lunch ${profile.lunch}, dinner ${profile.dinner}")
        appendLine("Activity level: ${profile.activityLevel}")
        appendLine()
        // Compact one-line reference. Gives the model concrete healthy windows
        // to suggest *toward* when it proposes rescheduling, instead of
        // inventing thresholds case-by-case.
        appendLine(HealthyDefaults.PROMPT_HINT)
    }

    private fun parseScheduleAnalysis(raw: String): ScheduleAnalysis? {
        return try {
            val dto = jsonParser.decodeFromString(ScheduleAnalysisDto.serializer(), raw)
            ScheduleAnalysis(
                hasConflict = dto.hasConflict,
                scheduleScore = dto.scheduleScore.coerceIn(0, 100),
                energyLevel = parseEnum<EnergyLevel>(dto.energyLevel) ?: EnergyLevel.MODERATE,
                burnoutRisk = parseEnum<BurnoutRisk>(dto.burnoutRisk) ?: BurnoutRisk.LOW,
                summary = dto.summary.trim(),
                positiveFeedback = dto.positiveFeedback.trim(),
                conflicts = dto.conflicts.take(3).mapNotNull { c ->
                    val parsedType = parseEnum<ConflictType>(c.type) ?: return@mapNotNull null
                    if (c.habitA.isBlank()) return@mapNotNull null
                    ScheduleConflict(
                        type = parsedType,
                        habitA = c.habitA.trim(),
                        habitB = c.habitB?.trim()?.takeIf { it.isNotEmpty() },
                        issue = c.issue.trim(),
                        suggestion = c.suggestion.trim()
                    )
                },
                optimizedSchedule = dto.optimizedSchedule.take(5).mapNotNull { o ->
                    if (o.habit.isBlank()) return@mapNotNull null
                    if (!o.suggestedTime.matches(HHMM_REGEX)) return@mapNotNull null
                    OptimizedHabitTime(habit = o.habit.trim(), suggestedTime = o.suggestedTime)
                },
                isCanned = false
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse schedule JSON: $raw", e)
            null
        }
    }

    /**
     * Enum-safe parser. Returns null instead of throwing so the caller can fall
     * back to a sensible default instead of dropping the whole analysis when
     * the model returns a value outside the pinned enum.
     */
    private inline fun <reified E : Enum<E>> parseEnum(raw: String?): E? {
        val cleaned = raw?.trim()?.uppercase() ?: return null
        return runCatching { enumValueOf<E>(cleaned) }.getOrNull()
    }

    /**
     * Empty-data response served when the user has fewer than 2 habits with
     * reminder times. Matches the prompt's contract so the UI doesn't need
     * a separate "no data" branch.
     */
    private fun emptyDataAnalysis(): ScheduleAnalysis = ScheduleAnalysis(
        hasConflict = false,
        scheduleScore = 100,
        energyLevel = EnergyLevel.MODERATE,
        burnoutRisk = BurnoutRisk.LOW,
        summary = "Chưa đủ dữ liệu để phân tích",
        positiveFeedback = "",
        conflicts = emptyList(),
        optimizedSchedule = emptyList(),
        isCanned = false
    )

    /**
     * Local deterministic fallback served when every OpenRouter model fails.
     *
     * Runs four rule-based detectors against the user's actual schedule + lifestyle
     * profile so the offline result feels believable instead of generic:
     *
     * 1. **OVERLAP** — any two consecutive reminders within 15 minutes.
     *    Cost: −10 / suggestion: dời một trong hai trễ hơn ~15 phút.
     * 2. **OVERLOAD** — 3+ reminders inside any sliding 90-minute window.
     *    Cost: −15 / suggestion: rút bớt 1 thói quen khỏi khung này.
     * 3. **LATE_NIGHT** — a reminder at or after 22:00. Cost: −10.
     * 4. **POOR_SLEEP** — sleep duration under 6h. Cost: −20.
     *
     * At most 3 conflicts surface (matches the prompt's contract). Score
     * starts at 100 and decrements per rule, floored at 0. Burnout risk
     * derives from rule severity: POOR_SLEEP → HIGH; LATE_NIGHT or
     * OVERLOAD → MODERATE; otherwise LOW. Mirrors the same thresholds the
     * remote prompt uses so the user can't tell offline from online by score.
     */
    private fun cannedScheduleAnalysis(
        habits: List<ScheduleHabitInput>,
        profile: UserLifestyleProfile
    ): ScheduleAnalysis {
        val sorted = habits
            .mapNotNull { h -> h.toMinutesOrNull()?.let { h to it } }
            .sortedBy { it.second }
        val detected = mutableListOf<ScheduleConflict>()
        var score = 100

        // (1) Pairwise overlap.
        sorted.zipWithNext().firstOrNull { (a, b) -> b.second - a.second <= 15 }?.let { (a, b) ->
            detected += ScheduleConflict(
                type = ConflictType.OVERLAP,
                habitA = a.first.title,
                habitB = b.first.title,
                issue = "Hai thói quen này cách nhau dưới 15 phút.",
                suggestion = "Hãy thử dời một trong hai ra xa hơn ~15 phút."
            )
            score -= 10
        }

        // (2) Sliding 90-min overload — find the first window containing 3+ reminders.
        run {
            for (i in sorted.indices) {
                val window = sorted.drop(i).takeWhile { it.second - sorted[i].second <= 90 }
                if (window.size >= 3) {
                    detected += ScheduleConflict(
                        type = ConflictType.OVERLOAD,
                        habitA = window[0].first.title,
                        habitB = window[1].first.title,
                        issue = "Có ${window.size} thói quen trong vòng 90 phút quanh ${window[0].first.reminderTime}.",
                        suggestion = "Cân nhắc dời một thói quen ra khỏi khung này để dễ thở hơn."
                    )
                    score -= 15
                    break
                }
            }
        }

        // (3) Late-night reminder. Threshold tracks HealthyDefaults.HARD_HABIT_LATEST_HOUR
        // (21:00) — the same boundary the system prompt uses, so offline and online
        // analyses don't disagree on what counts as "late".
        val lateBoundary = HealthyDefaults.HARD_HABIT_LATEST_HOUR * 60
        habits.firstOrNull { (it.toMinutesOrNull() ?: -1) >= lateBoundary }?.let { late ->
            detected += ScheduleConflict(
                type = ConflictType.LATE_NIGHT,
                habitA = late.title,
                habitB = null,
                issue = "Thói quen này được đặt khá muộn (sau ${HealthyDefaults.HARD_HABIT_LATEST_HOUR}:00).",
                suggestion = "Hãy thử dời sớm hơn ~1 tiếng để dễ phục hồi."
            )
            score -= 10
        }

        // (4) Sleep duration. Handles ranges that cross midnight (start > end).
        val sleepMinutes = sleepDurationMinutes(profile)
        if (sleepMinutes != null && sleepMinutes < 6 * 60) {
            detected += ScheduleConflict(
                type = ConflictType.POOR_SLEEP,
                habitA = "Giờ ngủ",
                habitB = null,
                issue = "Tổng thời gian ngủ của bạn dưới 6 tiếng.",
                suggestion = "Hãy cố gắng giữ giấc ngủ ít nhất 7 tiếng mỗi đêm."
            )
            score -= 20
        }

        val finalConflicts = detected.take(3)
        val burnout = when {
            finalConflicts.any { it.type == ConflictType.POOR_SLEEP } -> BurnoutRisk.HIGH
            finalConflicts.any { it.type == ConflictType.LATE_NIGHT } -> BurnoutRisk.MODERATE
            finalConflicts.any { it.type == ConflictType.OVERLOAD } -> BurnoutRisk.MODERATE
            else -> BurnoutRisk.LOW
        }
        val energy = when (burnout) {
            BurnoutRisk.HIGH -> EnergyLevel.LOW
            BurnoutRisk.MODERATE -> EnergyLevel.MODERATE
            BurnoutRisk.LOW -> EnergyLevel.HIGH
        }
        val summary = when {
            finalConflicts.isEmpty() -> "Lịch trình của bạn nhìn chung khá cân đối."
            finalConflicts.size == 1 -> "Lịch trình của bạn ổn, có một điểm nhỏ nên điều chỉnh."
            else -> "Lịch trình có ${finalConflicts.size} điểm cần điều chỉnh nhẹ."
        }

        return ScheduleAnalysis(
            hasConflict = finalConflicts.isNotEmpty(),
            scheduleScore = score.coerceAtLeast(0),
            energyLevel = energy,
            burnoutRisk = burnout,
            summary = summary,
            positiveFeedback = "Bạn đang duy trì lịch trình đều đặn — đó là điểm cộng quan trọng.",
            conflicts = finalConflicts,
            optimizedSchedule = emptyList(),
            isCanned = true
        )
    }

    /**
     * Total sleep duration in minutes given a profile, handling sleep that
     * crosses midnight (e.g. 22:00 → 06:00 = 480 minutes). Returns null when
     * either time string fails to parse, so the caller can skip the rule
     * rather than emit a misleading conflict.
     */
    private fun sleepDurationMinutes(profile: UserLifestyleProfile): Int? {
        val start = parseHhMm(profile.sleepStart) ?: return null
        val end = parseHhMm(profile.sleepEnd) ?: return null
        return if (end > start) end - start else (24 * 60 - start) + end
    }

    private fun parseHhMm(value: String): Int? {
        val parts = value.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    /**
     * Add [deltaMinutes] to an "HH:mm" string, returning the new "HH:mm" (or
     * null when the input is malformed). Wraps mod-1440 so 23:30 + 60 → 00:30.
     * Used by the canned creation analysis to propose a safe non-colliding
     * reminder slot.
     */
    private fun offsetHhMm(value: String, deltaMinutes: Int): String? {
        val base = parseHhMm(value) ?: return null
        val total = ((base + deltaMinutes) % (24 * 60) + (24 * 60)) % (24 * 60)
        return String.format("%02d:%02d", total / 60, total % 60)
    }

    private fun ScheduleHabitInput.toMinutesOrNull(): Int? {
        val parts = reminderTime.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }

    @Serializable
    private data class ScheduleAnalysisDto(
        val hasConflict: Boolean = false,
        val scheduleScore: Int = 0,
        val energyLevel: String = "MODERATE",
        val burnoutRisk: String = "LOW",
        val summary: String = "",
        val positiveFeedback: String = "",
        val conflicts: List<ScheduleConflictDto> = emptyList(),
        val optimizedSchedule: List<OptimizedHabitTimeDto> = emptyList()
    )

    @Serializable
    private data class ScheduleConflictDto(
        val type: String = "",
        val habitA: String = "",
        val habitB: String? = null,
        val issue: String = "",
        val suggestion: String = ""
    )

    @Serializable
    private data class OptimizedHabitTimeDto(
        val habit: String = "",
        val suggestedTime: String = ""
    )

    // ============================================================
    // ONBOARDING SUGGESTER
    // ============================================================
    override suspend fun suggestOnboardingHabits(
        profile: OnboardingProfile,
        lifestyle: UserLifestyleProfile?
    ): OnboardingSuggestion {
        val effectiveLifestyle = lifestyle ?: UserLifestyleProfile.Default
        val messages = listOf(
            ChatMessage(role = "system", content = ONBOARDING_SYSTEM_PROMPT),
            ChatMessage(role = "user", content = buildOnboardingUserPrompt(profile, effectiveLifestyle))
        )

        var lastFailure: String? = null
        for ((index, model) in FALLBACK_MODELS.withIndex()) {
            val attempt = tryOnboardingModel(model, messages)
            attempt.onSuccess { return it }
            lastFailure = attempt.exceptionOrNull()?.message
            Log.w(TAG, "Onboarding model[$index]=$model failed: $lastFailure")
        }

        Log.w(TAG, "All onboarding models exhausted — serving canned starter set")
        return cannedOnboardingSuggestion()
    }

    private suspend fun tryOnboardingModel(
        model: String,
        messages: List<ChatMessage>
    ): Result<OnboardingSuggestion> {
        Log.d(TAG, "Using model=$model")
        return try {
            val response = api.chatCompletion(
                ChatRequest(
                    model = model,
                    messages = messages,
                    // 6 habits × ~50 tokens each + summary + recommendedFocus +
                    // top-level fields ≈ 400 tokens. 500 leaves headroom for
                    // Vietnamese multi-byte expansion.
                    maxTokens = ONBOARDING_MAX_TOKENS,
                    temperature = TEMPERATURE
                )
            )
            if (response.error != null) {
                return Result.failure(
                    IllegalStateException(response.error.message ?: "AI từ chối yêu cầu")
                )
            }
            val content = response.choices.firstOrNull()?.message?.content?.trim()
            if (content.isNullOrBlank()) {
                return Result.failure(IllegalStateException("AI không trả lời"))
            }
            val cleaned = content
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = parseOnboardingSuggestion(cleaned)
                ?: return Result.failure(IllegalStateException("AI trả về dữ liệu sai định dạng"))
            Result.success(parsed)
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(IllegalStateException("Mạng chậm (504)"))
        } catch (e: HttpException) {
            Result.failure(IllegalStateException(extractHttpErrorMessage(e)))
        } catch (e: java.io.IOException) {
            Result.failure(IllegalStateException("Không thể kết nối đến AI"))
        } catch (e: Exception) {
            Log.e(TAG, "Onboarding request threw", e)
            Result.failure(e)
        }
    }

    private fun buildOnboardingUserPrompt(
        profile: OnboardingProfile,
        lifestyle: UserLifestyleProfile
    ): String = buildString {
        appendLine("Goals:")
        if (profile.goals.isEmpty()) appendLine("[]")
        else profile.goals.forEach { appendLine("- $it") }
        appendLine()

        if (profile.selectedCategories.isNotEmpty()) {
            appendLine("Selected categories:")
            appendLine(profile.selectedCategories.joinToString(", ") { it.name })
            appendLine()
        }

        appendLine("Sleep: ${lifestyle.sleepStart} → ${lifestyle.sleepEnd}")
        appendLine("Work: ${lifestyle.workStart} → ${lifestyle.workEnd}")
        appendLine("Meals: breakfast ${lifestyle.breakfast}, lunch ${lifestyle.lunch}, dinner ${lifestyle.dinner}")
        appendLine("Activity level: ${profile.activityLevel.ifBlank { lifestyle.activityLevel }}")
        appendLine("Experience level: ${profile.experienceLevel.name}")

        if (profile.existingHabitTitles.isNotEmpty()) {
            appendLine()
            appendLine("Existing habit titles:")
            appendLine(profile.existingHabitTitles.joinToString(", ") { "\"$it\"" })
        }
        if (profile.wellnessFlags.isNotEmpty()) {
            appendLine()
            appendLine("Wellness flags:")
            appendLine(profile.wellnessFlags.joinToString(", "))
        }
    }

    private fun parseOnboardingSuggestion(raw: String): OnboardingSuggestion? {
        return try {
            val dto = jsonParser.decodeFromString(OnboardingSuggestionDto.serializer(), raw)
            OnboardingSuggestion(
                summary = dto.summary.trim(),
                energyProfile = parseEnum<EnergyLevel>(dto.energyProfile) ?: EnergyLevel.MODERATE,
                recommendedFocus = dto.recommendedFocus.trim(),
                habits = dto.habits.take(6).mapNotNull { h ->
                    val category = HabitCategoryKey.fromStringOrNull(h.category) ?: return@mapNotNull null
                    val difficulty = parseEnum<Difficulty>(h.difficulty) ?: Difficulty.EASY
                    val priority = parseEnum<Priority>(h.priority) ?: Priority.MEDIUM
                    if (h.title.isBlank()) return@mapNotNull null
                    if (!h.reminderTime.matches(HHMM_REGEX)) return@mapNotNull null
                    OnboardingSuggestedHabit(
                        title = h.title.trim(),
                        emoji = h.emoji.ifBlank { "✨" }.trim(),
                        description = h.description.trim(),
                        category = category,
                        difficulty = difficulty,
                        priority = priority,
                        estimatedMinutes = h.estimatedMinutes.coerceIn(1, 120),
                        reminderTime = h.reminderTime,
                        motivation = h.motivation.trim()
                    )
                },
                isCanned = false
            ).takeIf { it.habits.isNotEmpty() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse onboarding JSON: $raw", e)
            null
        }
    }

    /**
     * Handwritten 4-habit beginner starter set served when every OpenRouter
     * model fails. All EASY, all ≤15 minutes, spread across morning / day /
     * evening so the user never lands on a blank screen.
     */
    private fun cannedOnboardingSuggestion(): OnboardingSuggestion = OnboardingSuggestion(
        summary = "Bộ thói quen khởi đầu nhẹ nhàng và bền vững cho bạn.",
        energyProfile = EnergyLevel.MODERATE,
        recommendedFocus = "Hãy bắt đầu với những thói quen ngắn để xây dựng đà đều đặn.",
        habits = listOf(
            OnboardingSuggestedHabit(
                title = "Uống 1 cốc nước sau khi thức",
                emoji = "💧",
                description = "Bù nước cho cơ thể ngay khi bắt đầu ngày mới.",
                category = HabitCategoryKey.HEALTH,
                difficulty = Difficulty.EASY,
                priority = Priority.MEDIUM,
                estimatedMinutes = 2,
                reminderTime = "07:30",
                motivation = "Một thói quen nhỏ tạo đà cho cả ngày."
            ),
            OnboardingSuggestedHabit(
                title = "Đi bộ 15 phút sau giờ làm",
                emoji = "🚶",
                description = "Vận động nhẹ giúp giãn cơ và làm dịu đầu óc.",
                category = HabitCategoryKey.FITNESS,
                difficulty = Difficulty.EASY,
                priority = Priority.MEDIUM,
                estimatedMinutes = 15,
                reminderTime = "18:00",
                motivation = "15 phút mỗi ngày tốt hơn 1 giờ mỗi tuần."
            ),
            OnboardingSuggestedHabit(
                title = "Đọc 10 phút trước khi ngủ",
                emoji = "📖",
                description = "Đọc gì cũng được — một cuốn sách bạn thích.",
                category = HabitCategoryKey.STUDY,
                difficulty = Difficulty.EASY,
                priority = Priority.LOW,
                estimatedMinutes = 10,
                reminderTime = "21:30",
                motivation = "10 phút mỗi tối tích lũy thành kiến thức bền."
            ),
            OnboardingSuggestedHabit(
                title = "Ghi 3 điều biết ơn",
                emoji = "📝",
                description = "Viết ngắn 3 điều bạn biết ơn hôm nay.",
                category = HabitCategoryKey.MINDFULNESS,
                difficulty = Difficulty.EASY,
                priority = Priority.LOW,
                estimatedMinutes = 5,
                reminderTime = "22:00",
                motivation = "Tâm trí nhẹ nhõm trước khi ngủ."
            )
        ),
        isCanned = true
    )

    @Serializable
    private data class OnboardingSuggestionDto(
        val summary: String = "",
        val energyProfile: String = "MODERATE",
        val recommendedFocus: String = "",
        val habits: List<OnboardingHabitDto> = emptyList()
    )

    @Serializable
    private data class OnboardingHabitDto(
        val title: String = "",
        val emoji: String = "",
        val description: String = "",
        val category: String = "",
        val difficulty: String = "EASY",
        val priority: String = "MEDIUM",
        val estimatedMinutes: Int = 10,
        val reminderTime: String = "",
        val motivation: String = ""
    )

    // ============================================================
    // ADAPTIVE LIFESTYLE INSIGHT ENGINE
    // ============================================================
    override suspend fun analyzeLifestyle(
        lifestyle: UserLifestyleProfile?,
        history: List<HabitCompletionRecord>,
        missedPatterns: List<String>,
        activeHabitTitles: List<String>,
        wellnessSignals: List<String>
    ): LifestyleInsight {
        val effectiveLifestyle = lifestyle ?: UserLifestyleProfile.Default
        val messages = listOf(
            ChatMessage(role = "system", content = LIFESTYLE_SYSTEM_PROMPT),
            ChatMessage(
                role = "user",
                content = buildLifestyleUserPrompt(
                    effectiveLifestyle,
                    history,
                    missedPatterns,
                    activeHabitTitles,
                    wellnessSignals
                )
            )
        )

        var lastFailure: String? = null
        for ((index, model) in FALLBACK_MODELS.withIndex()) {
            val attempt = tryLifestyleModel(model, messages)
            attempt.onSuccess { return it }
            lastFailure = attempt.exceptionOrNull()?.message
            Log.w(TAG, "Lifestyle model[$index]=$model failed: $lastFailure")
        }

        Log.w(TAG, "All lifestyle models exhausted — serving canned insight")
        return cannedLifestyleInsight(history)
    }

    private suspend fun tryLifestyleModel(
        model: String,
        messages: List<ChatMessage>
    ): Result<LifestyleInsight> {
        Log.d(TAG, "Using model=$model")
        return try {
            val response = api.chatCompletion(
                ChatRequest(
                    model = model,
                    messages = messages,
                    maxTokens = LIFESTYLE_MAX_TOKENS,
                    temperature = TEMPERATURE
                )
            )
            if (response.error != null) {
                return Result.failure(
                    IllegalStateException(response.error.message ?: "AI từ chối yêu cầu")
                )
            }
            val content = response.choices.firstOrNull()?.message?.content?.trim()
            if (content.isNullOrBlank()) {
                return Result.failure(IllegalStateException("AI không trả lời"))
            }
            val cleaned = content
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = parseLifestyleInsight(cleaned)
                ?: return Result.failure(IllegalStateException("AI trả về dữ liệu sai định dạng"))
            Result.success(parsed)
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(IllegalStateException("Mạng chậm (504)"))
        } catch (e: HttpException) {
            Result.failure(IllegalStateException(extractHttpErrorMessage(e)))
        } catch (e: java.io.IOException) {
            Result.failure(IllegalStateException("Không thể kết nối đến AI"))
        } catch (e: Exception) {
            Log.e(TAG, "Lifestyle request threw", e)
            Result.failure(e)
        }
    }

    private fun buildLifestyleUserPrompt(
        lifestyle: UserLifestyleProfile,
        history: List<HabitCompletionRecord>,
        missedPatterns: List<String>,
        activeHabitTitles: List<String>,
        wellnessSignals: List<String>
    ): String = buildString {
        appendLine("User profile:")
        appendLine("- Sleep: ${lifestyle.sleepStart} → ${lifestyle.sleepEnd}")
        appendLine("- Work: ${lifestyle.workStart} → ${lifestyle.workEnd}")
        appendLine("- Activity level: ${lifestyle.activityLevel}")
        appendLine()

        appendLine("Habit completion history (last 14 days):")
        if (history.isEmpty()) {
            appendLine("[]")
        } else {
            appendLine("[")
            history.forEachIndexed { i, r ->
                val comma = if (i < history.size - 1) "," else ""
                appendLine(
                    "  { \"title\": \"${r.title}\", \"completionRate\": ${r.completionRate}, " +
                        "\"preferredTime\": \"${r.preferredTime}\", " +
                        "\"difficulty\": \"${r.difficulty}\" }$comma"
                )
            }
            appendLine("]")
        }
        appendLine()

        appendLine("Missed patterns:")
        appendLine(
            if (missedPatterns.isEmpty()) "[]"
            else missedPatterns.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        )
        appendLine()

        appendLine("Current active habits:")
        appendLine(
            if (activeHabitTitles.isEmpty()) "[]"
            else activeHabitTitles.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        )
        appendLine()

        appendLine("Wellness signals:")
        appendLine(
            if (wellnessSignals.isEmpty()) "[]"
            else wellnessSignals.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        )
    }

    private fun parseLifestyleInsight(raw: String): LifestyleInsight? {
        return try {
            val dto = jsonParser.decodeFromString(LifestyleInsightDto.serializer(), raw)
            val parsedSuggestions = dto.adaptiveSuggestions.take(4).mapNotNull { s ->
                val type = parseEnum<SuggestionType>(s.type) ?: return@mapNotNull null
                if (s.title.isBlank()) return@mapNotNull null
                AdaptiveSuggestion(
                    type = type,
                    title = s.title.trim(),
                    reason = s.reason.trim(),
                    suggestion = s.suggestion.trim()
                )
            }
            // Empty arrays not allowed by the spec — drop the whole insight as
            // malformed if the model returned zero usable suggestions, the
            // canned fallback will produce a MAINTAIN_STABILITY row.
            if (parsedSuggestions.isEmpty()) return null
            LifestyleInsight(
                overallTrend = parseEnum<OverallTrend>(dto.overallTrend) ?: OverallTrend.STABLE,
                burnoutRisk = parseEnum<BurnoutRisk>(dto.burnoutRisk) ?: BurnoutRisk.LOW,
                consistencyScore = dto.consistencyScore.coerceIn(0, 100),
                energyPattern = parseEnum<EnergyPattern>(dto.energyPattern)
                    ?: EnergyPattern.INCONSISTENT,
                recoveryScore = dto.recoveryScore.coerceIn(0, 100),
                primaryInsight = dto.primaryInsight.trim(),
                coachingMessage = dto.coachingMessage.trim(),
                adaptiveSuggestions = parsedSuggestions,
                isCanned = false
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse lifestyle JSON: $raw", e)
            null
        }
    }

    /**
     * Deterministic local insight served when every OpenRouter model fails.
     * Reads the same 14-day history the prompt would have received and produces
     * a calibrated baseline: average completion rate drives the consistency
     * score; any habit ≥ 21:00 with sub-50 completion triggers an IMPROVE_SLEEP
     * suggestion; otherwise the user gets a MAINTAIN_STABILITY pat on the
     * back. Mirrors the prompt's coaching stance so offline and online
     * insights stay tonally aligned.
     */
    private fun cannedLifestyleInsight(history: List<HabitCompletionRecord>): LifestyleInsight {
        if (history.isEmpty()) {
            return LifestyleInsight(
                overallTrend = OverallTrend.STABLE,
                burnoutRisk = BurnoutRisk.LOW,
                consistencyScore = 70,
                energyPattern = EnergyPattern.INCONSISTENT,
                recoveryScore = 70,
                primaryInsight = "Hãy bắt đầu với một vài thói quen nhẹ để mình có dữ liệu phân tích.",
                coachingMessage = "Khi bạn check-in đều trong ít nhất 7 ngày, mình sẽ đưa ra gợi ý sát hơn với nhịp sống của bạn.",
                adaptiveSuggestions = listOf(
                    AdaptiveSuggestion(
                        type = SuggestionType.IMPROVE_CONSISTENCY,
                        title = "Bắt đầu với 2-3 thói quen nhỏ",
                        reason = "Chưa có dữ liệu hành vi để phân tích.",
                        suggestion = "Hãy chọn 2-3 thói quen ngắn và duy trì đều trong tuần đầu."
                    )
                ),
                isCanned = true
            )
        }

        val avgCompletion = history.map { it.completionRate }.average().toInt()
        val lateLowPerformers = history.filter { r ->
            val parts = r.preferredTime.split(":")
            val hour = parts.firstOrNull()?.toIntOrNull() ?: -1
            hour >= 21 && r.completionRate < 50
        }
        val trend = when {
            avgCompletion >= 75 -> OverallTrend.IMPROVING
            avgCompletion >= 50 -> OverallTrend.STABLE
            else -> OverallTrend.DECLINING
        }
        val burnout = when {
            avgCompletion < 40 -> BurnoutRisk.MODERATE
            lateLowPerformers.size >= 2 -> BurnoutRisk.MODERATE
            else -> BurnoutRisk.LOW
        }

        val suggestion = when {
            lateLowPerformers.isNotEmpty() -> AdaptiveSuggestion(
                type = SuggestionType.IMPROVE_SLEEP,
                title = "Cân nhắc dời thói quen tối sớm hơn",
                reason = "Bạn thường bỏ lỡ ${lateLowPerformers.size} thói quen sau 21:00.",
                suggestion = "Hãy thử dời các thói quen này sớm hơn 1-2 tiếng để dễ duy trì."
            )
            avgCompletion < 50 -> AdaptiveSuggestion(
                type = SuggestionType.SIMPLIFY_ROUTINE,
                title = "Đơn giản hoá lịch trình",
                reason = "Tỉ lệ hoàn thành trung bình $avgCompletion% — có thể bạn đang ôm hơi nhiều.",
                suggestion = "Tạm giảm còn 3-4 thói quen ưu tiên và giữ đều trong 2 tuần."
            )
            else -> AdaptiveSuggestion(
                type = SuggestionType.MAINTAIN_STABILITY,
                title = "Tiếp tục giữ nhịp hiện tại",
                reason = "Bạn đang duy trì khá đều ở mức $avgCompletion%.",
                suggestion = "Giữ nguyên các thói quen — sự nhất quán đáng giá hơn tăng cường độ."
            )
        }

        return LifestyleInsight(
            overallTrend = trend,
            burnoutRisk = burnout,
            consistencyScore = avgCompletion,
            energyPattern = EnergyPattern.INCONSISTENT,
            recoveryScore = (avgCompletion + 10).coerceAtMost(95),
            primaryInsight = when (trend) {
                OverallTrend.IMPROVING -> "Bạn đang giữ nhịp tốt với mức hoàn thành trung bình $avgCompletion%."
                OverallTrend.STABLE -> "Nhịp thói quen của bạn ổn định, có chỗ để cải thiện nhẹ."
                OverallTrend.DECLINING -> "Tuần qua hơi gấp với bạn — hãy nhẹ nhàng với chính mình."
            },
            coachingMessage = "Sự nhất quán quan trọng hơn cường độ. Hãy tập trung vào những thói quen bạn đã làm tốt thay vì thêm mới ngay.",
            adaptiveSuggestions = listOf(suggestion),
            isCanned = true
        )
    }

    @Serializable
    private data class LifestyleInsightDto(
        val overallTrend: String = "STABLE",
        val burnoutRisk: String = "LOW",
        val consistencyScore: Int = 0,
        val energyPattern: String = "INCONSISTENT",
        val recoveryScore: Int = 0,
        val primaryInsight: String = "",
        val coachingMessage: String = "",
        val adaptiveSuggestions: List<AdaptiveSuggestionDto> = emptyList()
    )

    @Serializable
    private data class AdaptiveSuggestionDto(
        val type: String = "",
        val title: String = "",
        val reason: String = "",
        val suggestion: String = ""
    )

    // ============================================================
    // HABIT CREATION ASSISTANT
    // ============================================================
    override suspend fun analyzeHabitCreation(input: HabitCreationInput): HabitCreationAnalysis {
        val messages = listOf(
            ChatMessage(role = "system", content = HABIT_CREATION_SYSTEM_PROMPT),
            ChatMessage(role = "user", content = buildHabitCreationUserPrompt(input))
        )

        var lastFailure: String? = null
        for ((index, model) in FALLBACK_MODELS.withIndex()) {
            val attempt = tryHabitCreationModel(model, messages)
            attempt.onSuccess { return it }
            lastFailure = attempt.exceptionOrNull()?.message
            Log.w(TAG, "HabitCreation model[$index]=$model failed: $lastFailure")
        }

        Log.w(TAG, "All habit-creation models exhausted — serving canned analysis")
        return cannedHabitCreationAnalysis(input)
    }

    private suspend fun tryHabitCreationModel(
        model: String,
        messages: List<ChatMessage>
    ): Result<HabitCreationAnalysis> {
        Log.d(TAG, "Using model=$model")
        return try {
            val response = api.chatCompletion(
                ChatRequest(
                    model = model,
                    messages = messages,
                    maxTokens = HABIT_CREATION_MAX_TOKENS,
                    temperature = TEMPERATURE
                )
            )
            if (response.error != null) {
                return Result.failure(
                    IllegalStateException(response.error.message ?: "AI từ chối yêu cầu")
                )
            }
            val content = response.choices.firstOrNull()?.message?.content?.trim()
            if (content.isNullOrBlank()) {
                return Result.failure(IllegalStateException("AI không trả lời"))
            }
            val cleaned = content
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = parseHabitCreationAnalysis(cleaned)
                ?: return Result.failure(IllegalStateException("AI trả về dữ liệu sai định dạng"))
            Result.success(parsed)
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(IllegalStateException("Mạng chậm (504)"))
        } catch (e: HttpException) {
            Result.failure(IllegalStateException(extractHttpErrorMessage(e)))
        } catch (e: java.io.IOException) {
            Result.failure(IllegalStateException("Không thể kết nối đến AI"))
        } catch (e: Exception) {
            Log.e(TAG, "HabitCreation request threw", e)
            Result.failure(e)
        }
    }

    private fun buildHabitCreationUserPrompt(input: HabitCreationInput): String = buildString {
        appendLine("New habit:")
        appendLine("- title: \"${input.newHabit.title}\"")
        if (input.newHabit.categoryName.isNotBlank())
            appendLine("- category: \"${input.newHabit.categoryName}\"")
        if (input.newHabit.reminderTime.isNotBlank())
            appendLine("- reminderTime: ${input.newHabit.reminderTime}")
        appendLine("- duration: ${input.newHabit.durationMinutes} min")
        appendLine("- difficulty: ${input.newHabit.difficulty}")
        appendLine("- frequency: ${input.newHabit.frequency}")
        appendLine()

        appendLine("Active habits (${input.activeHabits.size}):")
        if (input.activeHabits.isEmpty()) appendLine("[]")
        else input.activeHabits.forEach { h ->
            appendLine(
                "- \"${h.title}\" — ${h.categoryName}, ${h.reminderTime.ifBlank { "no reminder" }}, " +
                    "${h.difficulty}, ${h.durationMinutes} min, ${h.frequency}"
            )
        }
        appendLine()

        if (input.recentCompletion.isNotEmpty()) {
            appendLine("Recent 14-day completion:")
            input.recentCompletion.forEach { r ->
                appendLine("- \"${r.title}\": ${r.completionRate}%")
            }
            appendLine()
        }

        if (input.completedHabitTitles.isNotEmpty()) {
            appendLine("Previously completed (journey done):")
            appendLine(input.completedHabitTitles.joinToString(", ") { "\"$it\"" })
            appendLine()
        }
        if (input.archivedHabitTitles.isNotEmpty()) {
            appendLine("Archived / abandoned:")
            appendLine(input.archivedHabitTitles.joinToString(", ") { "\"$it\"" })
            appendLine()
        }

        appendLine("Lifestyle:")
        appendLine("- Sleep: ${input.lifestyle.sleepStart} → ${input.lifestyle.sleepEnd}")
        appendLine("- Work: ${input.lifestyle.workStart} → ${input.lifestyle.workEnd}")
    }

    private fun parseHabitCreationAnalysis(raw: String): HabitCreationAnalysis? {
        return try {
            val dto = jsonParser.decodeFromString(HabitCreationDto.serializer(), raw)
            val warnings = dto.warnings.take(3).mapNotNull { w ->
                val type = parseEnum<WarningType>(w.type) ?: return@mapNotNull null
                if (w.message.isBlank()) return@mapNotNull null
                HabitCreationWarning(type = type, message = w.message.trim())
            }
            val suggestions = dto.suggestions.take(3).mapNotNull { s ->
                val type = parseEnum<CreationSuggestionType>(s.type) ?: return@mapNotNull null
                if (s.message.isBlank()) return@mapNotNull null
                HabitCreationSuggestion(
                    type = type,
                    message = s.message.trim(),
                    suggestedTitle = s.suggestedTitle?.trim()?.takeIf { it.isNotEmpty() },
                    suggestedReminderTime = s.suggestedReminderTime?.trim()
                        ?.takeIf { it.matches(HHMM_REGEX) },
                    suggestedDifficulty = s.suggestedDifficulty?.trim()?.uppercase()
                        ?.takeIf { it in setOf("EASY", "MEDIUM", "HARD") },
                    suggestedFrequency = s.suggestedFrequency?.trim()?.takeIf { it.isNotEmpty() },
                    suggestedCategory = s.suggestedCategory?.trim()?.takeIf { it.isNotEmpty() },
                    suggestedDurationMinutes = s.suggestedDurationMinutes
                        ?.takeIf { it in 1..240 },
                    suggestedReplacementHabit = s.suggestedReplacementHabit?.trim()
                        ?.takeIf { it.isNotEmpty() }
                )
            }
            HabitCreationAnalysis(
                shouldWarn = dto.shouldWarn,
                overallRisk = parseEnum<CreationRiskLevel>(dto.overallRisk)
                    ?: CreationRiskLevel.LOW,
                warnings = warnings,
                suggestions = suggestions,
                encouragement = dto.encouragement.trim(),
                isCanned = false
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse habit-creation JSON: $raw", e)
            null
        }
    }

    /**
     * Rule-based local analysis served when every OpenRouter model fails.
     * Detects four conditions against the actual form input + user's existing
     * habits — same boundaries the system prompt uses, so the offline result
     * stays tonally aligned with what online would produce.
     *
     *   1. TIME_CONFLICT  — existing reminder within 15 min of the new one.
     *   2. SLEEP_CONFLICT — new reminder at or after [HealthyDefaults.HARD_HABIT_LATEST_HOUR].
     *   3. TOO_MANY_HABITS — user already has ≥ 8 active habits.
     *   4. DUPLICATE_INTENT — substring match of the new title against any existing.
     *
     * Encouragement and risk level are calibrated to the strongest warning
     * detected; if nothing fires we still produce a supportive "go for it"
     * line with an empty warning list so the UI surfaces nothing alarming.
     */
    private fun cannedHabitCreationAnalysis(input: HabitCreationInput): HabitCreationAnalysis {
        val warnings = mutableListOf<HabitCreationWarning>()
        val suggestions = mutableListOf<HabitCreationSuggestion>()

        val newMinutes = parseHhMm(input.newHabit.reminderTime)
        // (1) Time conflict — pairwise check against active habits.
        if (newMinutes != null) {
            val collision = input.activeHabits.firstOrNull { h ->
                val existing = parseHhMm(h.reminderTime) ?: return@firstOrNull false
                kotlin.math.abs(existing - newMinutes) <= 15
            }
            if (collision != null) {
                warnings += HabitCreationWarning(
                    WarningType.TIME_CONFLICT,
                    "Giờ nhắc mới sát với thói quen \"${collision.title}\" (${collision.reminderTime})."
                )
                // Suggest a slot 60 minutes away from the collision (or fall
                // back to a safe morning slot when arithmetic would wrap).
                val suggestedSlot = offsetHhMm(collision.reminderTime, deltaMinutes = 60)
                    ?: "07:00"
                suggestions += HabitCreationSuggestion(
                    type = CreationSuggestionType.CHANGE_TIME,
                    message = "Bạn có thể dời sang một khung khác cách ít nhất 15-30 phút để dễ duy trì cả hai.",
                    suggestedReminderTime = suggestedSlot
                )
            }
        }

        // (2) Sleep conflict — late-night reminder.
        if (newMinutes != null && newMinutes >= HealthyDefaults.HARD_HABIT_LATEST_HOUR * 60) {
            warnings += HabitCreationWarning(
                WarningType.SLEEP_CONFLICT,
                "Thói quen này khá muộn (sau ${HealthyDefaults.HARD_HABIT_LATEST_HOUR}:00) — có thể ảnh hưởng nhịp ngủ."
            )
            suggestions += HabitCreationSuggestion(
                type = CreationSuggestionType.CHANGE_TIME,
                message = "Hãy thử dời sớm hơn 1-2 tiếng để dễ phục hồi và ngủ ngon hơn.",
                suggestedReminderTime = "18:30"
            )
        }

        // (3) Too many habits.
        if (input.activeHabits.size >= 8) {
            warnings += HabitCreationWarning(
                WarningType.TOO_MANY_HABITS,
                "Bạn đang theo dõi ${input.activeHabits.size} thói quen — khá nhiều cho một ngày."
            )
            suggestions += HabitCreationSuggestion(
                CreationSuggestionType.START_SMALLER,
                "Cân nhắc tạm dừng 1-2 thói quen ít ưu tiên trước khi thêm cái mới."
            )
        }

        // (4) Duplicate intent — fuzzy title overlap.
        val newTitle = input.newHabit.title.trim().lowercase()
        if (newTitle.length >= 3) {
            val similar = input.activeHabits.firstOrNull { h ->
                val existing = h.title.trim().lowercase()
                existing.contains(newTitle) || newTitle.contains(existing)
            }
            if (similar != null) {
                warnings += HabitCreationWarning(
                    WarningType.DUPLICATE_INTENT,
                    "Bạn đã có một thói quen khá giống: \"${similar.title}\"."
                )
                suggestions += HabitCreationSuggestion(
                    type = CreationSuggestionType.REPLACE_EXISTING,
                    message = "Có thể nâng cấp thói quen hiện tại sẽ bền vững hơn là tạo thêm một thói quen mới.",
                    suggestedReplacementHabit = similar.title
                )
            }
        }

        val risk = when {
            warnings.any { it.type == WarningType.SLEEP_CONFLICT } -> CreationRiskLevel.MODERATE
            warnings.size >= 2 -> CreationRiskLevel.MODERATE
            warnings.isEmpty() -> CreationRiskLevel.LOW
            else -> CreationRiskLevel.LOW
        }
        val encouragement = when {
            warnings.isEmpty() -> "Một thói quen nhẹ nhàng nữa — chúc bạn duy trì đều đặn."
            else -> "Bắt đầu nhẹ sẽ giúp bạn duy trì lâu dài hơn."
        }
        return HabitCreationAnalysis(
            shouldWarn = warnings.isNotEmpty(),
            overallRisk = risk,
            warnings = warnings.take(3),
            suggestions = suggestions.take(3),
            encouragement = encouragement,
            isCanned = true
        )
    }

    @Serializable
    private data class HabitCreationDto(
        val shouldWarn: Boolean = false,
        val overallRisk: String = "LOW",
        val warnings: List<HabitCreationWarningDto> = emptyList(),
        val suggestions: List<HabitCreationSuggestionDto> = emptyList(),
        val encouragement: String = ""
    )

    @Serializable
    private data class HabitCreationWarningDto(
        val type: String = "",
        val message: String = ""
    )

    @Serializable
    private data class HabitCreationSuggestionDto(
        val type: String = "",
        val message: String = "",
        val suggestedTitle: String? = null,
        val suggestedReminderTime: String? = null,
        val suggestedDifficulty: String? = null,
        val suggestedFrequency: String? = null,
        val suggestedCategory: String? = null,
        val suggestedDurationMinutes: Int? = null,
        val suggestedReplacementHabit: String? = null
    )

    // ============================================================
    // ADAPTIVE HABIT RECOVERY ENGINE
    // ============================================================
    override suspend fun analyzeHabitRecovery(input: HabitRecoveryInput): HabitRecoveryAnalysis {
        val messages = listOf(
            ChatMessage(role = "system", content = RECOVERY_SYSTEM_PROMPT),
            ChatMessage(role = "user", content = buildRecoveryUserPrompt(input))
        )

        var lastFailure: String? = null
        for ((index, model) in FALLBACK_MODELS.withIndex()) {
            val attempt = tryRecoveryModel(model, messages)
            attempt.onSuccess { return it }
            lastFailure = attempt.exceptionOrNull()?.message
            Log.w(TAG, "Recovery model[$index]=$model failed: $lastFailure")
        }

        Log.w(TAG, "All recovery models exhausted — serving canned plan")
        return cannedRecoveryAnalysis(input)
    }

    private suspend fun tryRecoveryModel(
        model: String,
        messages: List<ChatMessage>
    ): Result<HabitRecoveryAnalysis> {
        Log.d(TAG, "Using model=$model")
        return try {
            val response = api.chatCompletion(
                ChatRequest(
                    model = model,
                    messages = messages,
                    maxTokens = RECOVERY_MAX_TOKENS,
                    temperature = TEMPERATURE
                )
            )
            if (response.error != null) {
                return Result.failure(
                    IllegalStateException(response.error.message ?: "AI từ chối yêu cầu")
                )
            }
            val content = response.choices.firstOrNull()?.message?.content?.trim()
            if (content.isNullOrBlank()) {
                return Result.failure(IllegalStateException("AI không trả lời"))
            }
            val cleaned = content
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = parseRecoveryAnalysis(cleaned)
                ?: return Result.failure(IllegalStateException("AI trả về dữ liệu sai định dạng"))
            Result.success(parsed)
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(IllegalStateException("Mạng chậm (504)"))
        } catch (e: HttpException) {
            Result.failure(IllegalStateException(extractHttpErrorMessage(e)))
        } catch (e: java.io.IOException) {
            Result.failure(IllegalStateException("Không thể kết nối đến AI"))
        } catch (e: Exception) {
            Log.e(TAG, "Recovery request threw", e)
            Result.failure(e)
        }
    }

    private fun buildRecoveryUserPrompt(input: HabitRecoveryInput): String = buildString {
        appendLine("Active habits (${input.activeHabitCount}):")
        if (input.allHabitStats.isEmpty()) appendLine("[]")
        else input.allHabitStats.forEach { row ->
            val delta = row.completionRate7d - row.previousWeekCompletionRate
            val trend = when {
                row.previousWeekCompletionRate == 0 && row.completionRate7d == 0 -> "no-data"
                delta >= 10 -> "improving (+$delta)"
                delta <= -10 -> "drifting ($delta)"
                else -> "stable"
            }
            appendLine(
                "- \"${row.title}\" — ${row.reminderTime.ifBlank { "no reminder" }}, " +
                    "${row.difficulty}, 7d=${row.completionRate7d}%, " +
                    "14d=${row.completionRate14d}%, prev-week=${row.previousWeekCompletionRate}%, " +
                    "trend=$trend, missStreak=${row.missStreak}"
            )
        }
        appendLine()
        appendLine("Struggling habits flagged by the trigger heuristics:")
        appendLine(
            if (input.strugglingTitles.isEmpty()) "[]"
            else input.strugglingTitles.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        )
        appendLine()
        appendLine("Detected triggers:")
        appendLine(
            if (input.detectedTriggers.isEmpty()) "[]"
            else input.detectedTriggers.joinToString(prefix = "[", postfix = "]") { "\"${it.name}\"" }
        )
        appendLine()
        if (input.lateNightHabitTitles.isNotEmpty()) {
            appendLine("Late-night habits (reminder ≥ 21:00):")
            appendLine(input.lateNightHabitTitles.joinToString(prefix = "[", postfix = "]") { "\"$it\"" })
            appendLine()
        }
        appendLine("Lifestyle:")
        appendLine("- Sleep: ${input.lifestyle.sleepStart} → ${input.lifestyle.sleepEnd}")
        appendLine("- Work: ${input.lifestyle.workStart} → ${input.lifestyle.workEnd}")
    }

    private fun parseRecoveryAnalysis(raw: String): HabitRecoveryAnalysis? {
        return try {
            val dto = jsonParser.decodeFromString(HabitRecoveryDto.serializer(), raw)
            val triggers = dto.triggerReasons.mapNotNull { parseEnum<RecoveryTrigger>(it) }
            val struggling = dto.struggling.take(5).mapNotNull { s ->
                if (s.title.isBlank()) return@mapNotNull null
                StrugglingHabit(
                    title = s.title.trim(),
                    completionRate7d = s.completionRate7d.coerceIn(0, 100),
                    completionRate14d = s.completionRate14d.coerceIn(0, 100),
                    missStreak = s.missStreak.coerceAtLeast(0),
                    recoveryReason = s.recoveryReason.trim()
                )
            }
            val actions = dto.recoveryActions.take(4).mapNotNull { a ->
                val type = parseEnum<RecoveryActionType>(a.type) ?: return@mapNotNull null
                if (a.title.isBlank()) return@mapNotNull null
                HabitRecoveryAction(
                    type = type,
                    targetHabit = a.targetHabit?.trim()?.takeIf { it.isNotEmpty() },
                    title = a.title.trim(),
                    description = a.description.trim(),
                    suggestedValue = a.suggestedValue.trim()
                )
            }
            HabitRecoveryAnalysis(
                shouldRecover = dto.shouldRecover,
                triggerReasons = triggers,
                overallTone = parseEnum<RecoveryIntensity>(dto.overallTone) ?: RecoveryIntensity.LIGHT,
                coachingMessage = dto.coachingMessage.trim(),
                struggling = struggling,
                recoveryActions = actions,
                isCanned = false
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse recovery JSON: $raw", e)
            null
        }
    }

    /**
     * Local recovery plan when every OpenRouter model fails. Builds the same
     * shape the prompt would have produced, calibrated from the use case's
     * pre-computed triggers + stats:
     *
     *   - Each habit with `missStreak ≥ 3` becomes a [StrugglingHabit] row with
     *     a calibrated `recoveryReason`.
     *   - At least one [HabitRecoveryAction] is emitted per detected trigger,
     *     capped at 4 total.
     *   - [RecoveryIntensity] derives from the worst signal — LIGHT for a
     *     single low-completion habit, MODERATE for multiple, AGGRESSIVE when
     *     burnout / hard-failing habits / late-night failures stack up.
     */
    private fun cannedRecoveryAnalysis(input: HabitRecoveryInput): HabitRecoveryAnalysis {
        val struggling = input.allHabitStats
            .filter { it.missStreak >= 3 || it.completionRate14d < 40 || it.title in input.strugglingTitles }
            .take(5)
            .map { row ->
                val reason = when {
                    row.missStreak >= 5 -> "Đã lỡ $row.missStreak ngày liên tiếp."
                    row.completionRate14d < 30 -> "Tỉ lệ hoàn thành 2 tuần chỉ ${row.completionRate14d}%."
                    row.difficulty == "HARD" -> "Cường độ HARD đang khó duy trì."
                    else -> "Có dấu hiệu bị quá tải."
                }
                StrugglingHabit(
                    title = row.title,
                    completionRate7d = row.completionRate7d,
                    completionRate14d = row.completionRate14d,
                    missStreak = row.missStreak,
                    recoveryReason = reason
                )
            }

        val actions = mutableListOf<HabitRecoveryAction>()
        val firstStruggling = struggling.firstOrNull()

        if (RecoveryTrigger.HARD_HABIT_FAILING in input.detectedTriggers && firstStruggling != null) {
            actions += HabitRecoveryAction(
                type = RecoveryActionType.REDUCE_DIFFICULTY,
                targetHabit = firstStruggling.title,
                title = "Giảm độ khó tạm thời",
                description = "Hãy thử phiên bản nhẹ hơn của thói quen này trong 1-2 tuần để khôi phục đà.",
                suggestedValue = ""
            )
        }
        if (RecoveryTrigger.LOW_COMPLETION in input.detectedTriggers && firstStruggling != null) {
            actions += HabitRecoveryAction(
                type = RecoveryActionType.REDUCE_DURATION,
                targetHabit = firstStruggling.title,
                title = "Rút ngắn thời lượng",
                description = "Bắt đầu lại với một phiên ngắn hơn — duy trì đều quan trọng hơn dài.",
                suggestedValue = "10 phút"
            )
        }
        if (RecoveryTrigger.LATE_NIGHT_FAILURES in input.detectedTriggers) {
            val late = input.lateNightHabitTitles.firstOrNull()
            actions += HabitRecoveryAction(
                type = RecoveryActionType.CHANGE_TIME,
                targetHabit = late,
                title = "Dời sớm hơn ${HealthyDefaults.HARD_HABIT_LATEST_HOUR}:00",
                description = "Thói quen muộn thường khó hoàn thành — hãy thử khung giờ sớm hơn.",
                suggestedValue = "19:00"
            )
        }
        if (RecoveryTrigger.TOO_MANY_HABITS in input.detectedTriggers) {
            actions += HabitRecoveryAction(
                type = RecoveryActionType.PAUSE_TEMPORARILY,
                targetHabit = null,
                title = "Tạm dừng 1-2 thói quen ít ưu tiên",
                description = "Tập trung vào ${input.strugglingTitles.size.coerceAtLeast(2)} thói quen quan trọng nhất sẽ bền vững hơn.",
                suggestedValue = ""
            )
        }
        if (actions.isEmpty() && firstStruggling != null) {
            actions += HabitRecoveryAction(
                type = RecoveryActionType.ADD_RECOVERY_HABIT,
                targetHabit = null,
                title = "Thêm thói quen phục hồi nhẹ",
                description = "Một thói quen ngắn như uống nước hoặc giãn cơ giúp bạn lấy lại nhịp.",
                suggestedValue = ""
            )
        }

        val tone = when {
            input.detectedTriggers.any {
                it == RecoveryTrigger.BURNOUT_RISK ||
                    it == RecoveryTrigger.HARD_HABIT_FAILING ||
                    it == RecoveryTrigger.CONSECUTIVE_FAILS
            } -> RecoveryIntensity.AGGRESSIVE
            input.detectedTriggers.size >= 2 -> RecoveryIntensity.MODERATE
            else -> RecoveryIntensity.LIGHT
        }
        // Week-over-week trend across the whole stat set. Used to add a
        // tiny longitudinal clause to the canned message — "đang khôi phục
        // dần" reads very differently from "đang xấu đi".
        val avgDelta = if (input.allHabitStats.isNotEmpty()) {
            input.allHabitStats.map { it.completionRate7d - it.previousWeekCompletionRate }
                .average().toInt()
        } else 0
        val trendClause = when {
            avgDelta >= 10 -> " Khá hơn tuần trước một chút —"
            avgDelta <= -10 -> " Đang chững so với tuần trước —"
            else -> ""
        }
        val message = when (tone) {
            RecoveryIntensity.AGGRESSIVE ->
                "${trendClause.ifBlank { "" }} Bạn đang khá đuối — hãy giảm tải để hồi phục, đừng tự trách nhé.".trim()
            RecoveryIntensity.MODERATE ->
                "${trendClause.ifBlank { "" }} Lịch trình đang hơi nặng — vài điều chỉnh nhỏ sẽ giúp bạn duy trì bền hơn.".trim()
            RecoveryIntensity.LIGHT ->
                "${trendClause.ifBlank { "" }} Một vài thói quen đang chững lại — nghỉ ngơi nhẹ và tiếp tục sẽ ổn thôi.".trim()
        }

        return HabitRecoveryAnalysis(
            shouldRecover = struggling.isNotEmpty() || input.detectedTriggers.isNotEmpty(),
            triggerReasons = input.detectedTriggers,
            overallTone = tone,
            coachingMessage = message,
            struggling = struggling,
            recoveryActions = actions.take(4),
            isCanned = true
        )
    }

    @Serializable
    private data class HabitRecoveryDto(
        val shouldRecover: Boolean = false,
        val triggerReasons: List<String> = emptyList(),
        val overallTone: String = "LIGHT",
        val coachingMessage: String = "",
        val struggling: List<StrugglingDto> = emptyList(),
        val recoveryActions: List<RecoveryActionDto> = emptyList()
    )

    @Serializable
    private data class StrugglingDto(
        val title: String = "",
        val completionRate7d: Int = 0,
        val completionRate14d: Int = 0,
        val missStreak: Int = 0,
        val recoveryReason: String = ""
    )

    @Serializable
    private data class RecoveryActionDto(
        val type: String = "",
        val targetHabit: String? = null,
        val title: String = "",
        val description: String = "",
        val suggestedValue: String = ""
    )

    // ============================================================
    // SMART HABIT PROGRESSION ENGINE
    // ============================================================
    override suspend fun analyzeHabitProgression(input: HabitProgressionInput): HabitProgressionAnalysis {
        val messages = listOf(
            ChatMessage(role = "system", content = PROGRESSION_SYSTEM_PROMPT),
            ChatMessage(role = "user", content = buildProgressionUserPrompt(input))
        )

        var lastFailure: String? = null
        for ((index, model) in FALLBACK_MODELS.withIndex()) {
            val attempt = tryProgressionModel(model, messages)
            attempt.onSuccess { return it }
            lastFailure = attempt.exceptionOrNull()?.message
            Log.w(TAG, "Progression model[$index]=$model failed: $lastFailure")
        }

        Log.w(TAG, "All progression models exhausted — serving canned plan")
        return cannedProgressionAnalysis(input)
    }

    private suspend fun tryProgressionModel(
        model: String,
        messages: List<ChatMessage>
    ): Result<HabitProgressionAnalysis> {
        Log.d(TAG, "Using model=$model")
        return try {
            val response = api.chatCompletion(
                ChatRequest(
                    model = model,
                    messages = messages,
                    maxTokens = PROGRESSION_MAX_TOKENS,
                    temperature = TEMPERATURE
                )
            )
            if (response.error != null) {
                return Result.failure(
                    IllegalStateException(response.error.message ?: "AI từ chối yêu cầu")
                )
            }
            val content = response.choices.firstOrNull()?.message?.content?.trim()
            if (content.isNullOrBlank()) {
                return Result.failure(IllegalStateException("AI không trả lời"))
            }
            val cleaned = content
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = parseProgressionAnalysis(cleaned)
                ?: return Result.failure(IllegalStateException("AI trả về dữ liệu sai định dạng"))
            Result.success(parsed)
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(IllegalStateException("Mạng chậm (504)"))
        } catch (e: HttpException) {
            Result.failure(IllegalStateException(extractHttpErrorMessage(e)))
        } catch (e: java.io.IOException) {
            Result.failure(IllegalStateException("Không thể kết nối đến AI"))
        } catch (e: Exception) {
            Log.e(TAG, "Progression request threw", e)
            Result.failure(e)
        }
    }

    private fun buildProgressionUserPrompt(input: HabitProgressionInput): String = buildString {
        appendLine("Active habits (${input.activeHabitCount}, HARD=${input.hardHabitCount}):")
        if (input.allHabitStats.isEmpty()) appendLine("[]")
        else input.allHabitStats.forEach { row ->
            val delta = row.completionRate7d - row.previousWeekCompletionRate
            val trend = when {
                row.previousWeekCompletionRate == 0 && row.completionRate7d == 0 -> "no-data"
                delta >= 10 -> "improving (+$delta)"
                delta <= -10 -> "easing back ($delta)"
                else -> "holding steady"
            }
            appendLine(
                "- \"${row.title}\" — ${row.reminderTime.ifBlank { "no reminder" }}, " +
                    "${row.difficulty}, 7d=${row.completionRate7d}%, " +
                    "14d=${row.completionRate14d}%, prev-week=${row.previousWeekCompletionRate}%, " +
                    "trend=$trend, streak=${row.currentStreak}d"
            )
        }
        appendLine()
        appendLine("Vibrant (≥85% over 14d):")
        appendLine(
            if (input.vibrantTitles.isEmpty()) "[]"
            else input.vibrantTitles.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        )
        appendLine()
        appendLine("Detected progression triggers:")
        appendLine(
            if (input.detectedTriggers.isEmpty()) "[]"
            else input.detectedTriggers.joinToString(prefix = "[", postfix = "]") { "\"${it.name}\"" }
        )
        appendLine()
        appendLine("Lifestyle:")
        appendLine("- Sleep: ${input.lifestyle.sleepStart} → ${input.lifestyle.sleepEnd}")
        appendLine("- Work: ${input.lifestyle.workStart} → ${input.lifestyle.workEnd}")
    }

    private fun parseProgressionAnalysis(raw: String): HabitProgressionAnalysis? {
        return try {
            val dto = jsonParser.decodeFromString(HabitProgressionDto.serializer(), raw)
            val triggers = dto.triggerReasons.mapNotNull { parseEnum<ProgressionTrigger>(it) }
            val vibrant = dto.vibrant.take(5).mapNotNull { v ->
                if (v.title.isBlank()) return@mapNotNull null
                VibrantHabit(
                    title = v.title.trim(),
                    completionRate7d = v.completionRate7d.coerceIn(0, 100),
                    completionRate14d = v.completionRate14d.coerceIn(0, 100),
                    currentStreak = v.currentStreak.coerceAtLeast(0),
                    readinessReason = v.readinessReason.trim()
                )
            }
            val actions = dto.progressionActions.take(4).mapNotNull { a ->
                val type = parseEnum<ProgressionActionType>(a.type) ?: return@mapNotNull null
                if (a.title.isBlank()) return@mapNotNull null
                HabitProgressionAction(
                    type = type,
                    targetHabit = a.targetHabit?.trim()?.takeIf { it.isNotEmpty() },
                    title = a.title.trim(),
                    description = a.description.trim(),
                    suggestedValue = a.suggestedValue.trim()
                )
            }
            HabitProgressionAnalysis(
                shouldProgress = dto.shouldProgress,
                triggerReasons = triggers,
                overallPace = parseEnum<ProgressionPace>(dto.overallPace) ?: ProgressionPace.GENTLE,
                coachingMessage = dto.coachingMessage.trim(),
                vibrant = vibrant,
                progressionActions = actions,
                isCanned = false
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse progression JSON: $raw", e)
            null
        }
    }

    /**
     * Local progression plan when every OpenRouter model fails. Derived from
     * the same vibrant stats the prompt would have consumed:
     *
     *   - Top habits with 14d ≥ 85% become [VibrantHabit] rows.
     *   - Suggestions are conservative: a duration bump for the top habit,
     *     a complementary supportive habit, and a positive reinforcement
     *     line. Never aggressive — canned content respects the same spec
     *     constraints the AI does.
     *   - Pace defaults to GENTLE; promotes to STEADY only when the top
     *     habit has ≥ 21-day streak.
     */
    private fun cannedProgressionAnalysis(input: HabitProgressionInput): HabitProgressionAnalysis {
        val vibrantRows = input.allHabitStats
            .filter { it.completionRate14d >= 85 }
            .sortedByDescending { it.currentStreak }
            .take(5)
            .map { row ->
                val reason = when {
                    row.currentStreak >= 21 -> "Duy trì ${row.currentStreak} ngày liền — nền tảng đã rất chắc."
                    row.currentStreak >= 14 -> "Đã giữ vững 2 tuần — sẵn sàng cho bước tiếp theo nhẹ nhàng."
                    row.completionRate14d >= 90 -> "Hoàn thành ${row.completionRate14d}% trong 14 ngày — nhịp đang tốt."
                    else -> "Tỉ lệ ổn định — có thể nâng nhẹ độ thử thách."
                }
                VibrantHabit(
                    title = row.title,
                    completionRate7d = row.completionRate7d,
                    completionRate14d = row.completionRate14d,
                    currentStreak = row.currentStreak,
                    readinessReason = reason
                )
            }

        val actions = mutableListOf<HabitProgressionAction>()
        val topHabit = vibrantRows.firstOrNull()
        if (topHabit != null) {
            actions += HabitProgressionAction(
                type = ProgressionActionType.INCREASE_DURATION,
                targetHabit = topHabit.title,
                title = "Tăng nhẹ thời lượng",
                description = "Bạn đã duy trì rất tốt — thử kéo dài thêm 5 phút mỗi lần để tiếp tục phát triển.",
                suggestedValue = "+5 phút"
            )
            if (topHabit.currentStreak >= 14 && input.activeHabitCount <= 5) {
                actions += HabitProgressionAction(
                    type = ProgressionActionType.ADD_COMPLEMENTARY_HABIT,
                    targetHabit = null,
                    title = "Thêm thói quen bổ trợ nhẹ",
                    description = "Một thói quen ngắn bổ trợ (uống nước, hít thở sâu) sẽ làm nhịp hiện tại cân bằng hơn.",
                    suggestedValue = ""
                )
            }
            if (vibrantRows.size >= 2) {
                actions += HabitProgressionAction(
                    type = ProgressionActionType.INCREASE_FREQUENCY,
                    targetHabit = vibrantRows[1].title,
                    title = "Thêm 1 ngày trong tuần",
                    description = "Nhịp đang ổn — có thể nâng tần suất nhẹ mà vẫn dễ duy trì.",
                    suggestedValue = "+1 lần/tuần"
                )
            }
        }
        actions += HabitProgressionAction(
            type = ProgressionActionType.CONSISTENCY_REWARD,
            targetHabit = null,
            title = "Bạn đang làm rất tốt",
            description = "Giữ vững chuỗi hiện tại đã là một thành tích — phát triển bền vững quan trọng hơn tốc độ.",
            suggestedValue = ""
        )

        val pace = if ((topHabit?.currentStreak ?: 0) >= 21) ProgressionPace.STEADY
        else ProgressionPace.GENTLE
        // Week-over-week delta across the vibrant set. Lets the canned
        // message acknowledge upward momentum specifically when it exists.
        val avgDelta = if (vibrantRows.isNotEmpty()) {
            input.allHabitStats
                .filter { row -> vibrantRows.any { it.title == row.title } }
                .map { it.completionRate7d - it.previousWeekCompletionRate }
                .average().toInt()
        } else 0
        val message = when {
            pace == ProgressionPace.STEADY ->
                "Bạn đã rất ổn định trong 3 tuần qua — có thể nâng nhẹ độ thử thách để tiếp tục phát triển."
            avgDelta >= 10 ->
                "Tuần này bạn nhất quán hơn tuần trước — một bước nhỏ tiếp theo sẽ vừa sức và bền vững."
            else ->
                "Bạn đang duy trì rất tốt — một bước nhỏ tiếp theo sẽ vừa sức và bền vững."
        }

        return HabitProgressionAnalysis(
            shouldProgress = vibrantRows.isNotEmpty(),
            triggerReasons = input.detectedTriggers,
            overallPace = pace,
            coachingMessage = message,
            vibrant = vibrantRows,
            progressionActions = actions.take(4),
            isCanned = true
        )
    }

    @Serializable
    private data class HabitProgressionDto(
        val shouldProgress: Boolean = false,
        val triggerReasons: List<String> = emptyList(),
        val overallPace: String = "GENTLE",
        val coachingMessage: String = "",
        val vibrant: List<VibrantDto> = emptyList(),
        val progressionActions: List<ProgressionActionDto> = emptyList()
    )

    @Serializable
    private data class VibrantDto(
        val title: String = "",
        val completionRate7d: Int = 0,
        val completionRate14d: Int = 0,
        val currentStreak: Int = 0,
        val readinessReason: String = ""
    )

    @Serializable
    private data class ProgressionActionDto(
        val type: String = "",
        val targetHabit: String? = null,
        val title: String = "",
        val description: String = "",
        val suggestedValue: String = ""
    )

    // ============================================================
    // RETRY POLICY
    // ============================================================
    /**
     * One retry on transient failures only. 429/500/502/503/504 and read timeouts
     * are worth a second swing — auth, payment, and "model not found" are not.
     */
    private suspend fun <T> retryOnTransient(block: suspend () -> T): T {
        val first = block()
        if (!isTransientFailure(first)) return first
        Log.d(TAG, "Transient failure — retrying once after ${RETRY_DELAY_MS}ms backoff")
        delay(RETRY_DELAY_MS)
        return block()
    }

    private fun isTransientFailure(result: Any?): Boolean = when (result) {
        is AiResult.Failure -> result.message.containsTransientCode()
        is AiSuggestResult.Failure -> result.message.containsTransientCode()
        else -> false
    }

    private fun String.containsTransientCode(): Boolean =
        contains("(429)") || contains("(500)") || contains("(502)") ||
            contains("(503)") || contains("(504)") || contains("Mạng chậm")

    // ============================================================
    // HTTP — REVIEW (plain text)
    // ============================================================
    private suspend fun tryModel(
        model: String,
        messages: List<ChatMessage>
    ): AiResult {
        Log.d(TAG, "Using model=$model")
        return try {
            val response = api.chatCompletion(
                ChatRequest(
                    model = model,
                    messages = messages,
                    maxTokens = MAX_TOKENS,
                    temperature = TEMPERATURE
                )
            )
            if (response.error != null) {
                return AiResult.Failure(
                    response.error.message ?: "Yêu cầu AI bị từ chối"
                )
            }
            val content = response.choices.firstOrNull()?.message?.content?.trim()
            if (content.isNullOrBlank()) {
                AiResult.Failure("AI không trả lời — vui lòng thử lại")
            } else {
                AiResult.Success(content)
            }
        } catch (e: java.net.SocketTimeoutException) {
            AiResult.Failure("Mạng chậm — AI hết thời gian chờ")
        } catch (e: HttpException) {
            AiResult.Failure(extractHttpErrorMessage(e))
        } catch (e: java.io.IOException) {
            AiResult.Failure("Không thể kết nối đến AI — kiểm tra mạng")
        } catch (e: Exception) {
            Log.e(TAG, "AI request threw", e)
            AiResult.Failure("Lỗi AI: ${e.message ?: "không xác định"}")
        }
    }

    // ============================================================
    // HTTP — SUGGESTIONS (JSON)
    // ============================================================
    private suspend fun tryModelJson(
        model: String,
        messages: List<ChatMessage>
    ): AiSuggestResult {
        Log.d(TAG, "Using model=$model")
        return try {
            val response = api.chatCompletion(
                ChatRequest(
                    model = model,
                    messages = messages,
                    maxTokens = MAX_TOKENS,
                    temperature = TEMPERATURE
                )
            )
            if (response.error != null) {
                return AiSuggestResult.Failure(
                    response.error.message ?: "Yêu cầu AI bị từ chối"
                )
            }
            val content = response.choices.firstOrNull()?.message?.content?.trim()
            if (content.isNullOrBlank()) {
                return AiSuggestResult.Failure("AI không trả lời — vui lòng thử lại")
            }
            // Free-tier models occasionally wrap JSON in ```json fences despite our
            // explicit ask. Strip them defensively before parsing.
            val cleaned = content
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            parseSuggestions(cleaned)
        } catch (e: java.net.SocketTimeoutException) {
            AiSuggestResult.Failure("Mạng chậm — AI hết thời gian chờ")
        } catch (e: HttpException) {
            AiSuggestResult.Failure(extractHttpErrorMessage(e))
        } catch (e: java.io.IOException) {
            AiSuggestResult.Failure("Không thể kết nối đến AI — kiểm tra mạng")
        } catch (e: Exception) {
            Log.e(TAG, "Suggest request threw", e)
            AiSuggestResult.Failure("Lỗi AI: ${e.message ?: "không xác định"}")
        }
    }

    private fun parseSuggestions(raw: String): AiSuggestResult {
        return try {
            val parsed = jsonParser.decodeFromString(SuggestionsEnvelope.serializer(), raw)
            val suggestions = parsed.suggestions
                .filter { it.title.isNotBlank() }
                .map {
                    SuggestedHabit(
                        title = it.title.trim(),
                        emoji = it.emoji.ifBlank { "✨" }.trim(),
                        description = it.description.trim(),
                        difficulty = it.difficulty.uppercase().let { d ->
                            if (d in setOf("EASY", "MEDIUM", "HARD")) d else "MEDIUM"
                        },
                        estimatedImpact = it.estimatedImpact.trim(),
                        streakBenefit = it.streakBenefit.trim()
                    )
                }
            if (suggestions.isEmpty()) {
                AiSuggestResult.Failure("AI không tạo được gợi ý — thử lại sau")
            } else {
                AiSuggestResult.Success(suggestions)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse AI suggestions JSON: $raw", e)
            AiSuggestResult.Failure("AI trả về dữ liệu không đúng định dạng — thử lại")
        }
    }

    // ============================================================
    // ERROR MAPPING
    // ============================================================
    /**
     * Pulls the OpenRouter error JSON out of an [HttpException] and returns a
     * Vietnamese reason mapped per status code.
     *
     * OpenRouter error body shape (per docs):
     * `{ "error": { "message": "No auth credentials found", "code": 401 } }`
     */
    private fun extractHttpErrorMessage(e: HttpException): String {
        val code = e.code()
        val rawBody = try {
            e.response()?.errorBody()?.string().orEmpty()
        } catch (_: Throwable) {
            ""
        }
        Log.w(TAG, "OpenRouter HTTP $code body=$rawBody")

        val parsed = runCatching {
            jsonParser.decodeFromString(ErrorEnvelope.serializer(), rawBody).error?.message
        }.getOrNull()

        val detail = parsed?.takeIf { it.isNotBlank() } ?: rawBody.take(160)

        return when (code) {
            401 -> "Khóa AI không hợp lệ (401). Kiểm tra OPENROUTER_API_KEY trong local.properties và build lại."
            402 -> "Tài khoản OpenRouter cần nạp credit (402)${if (detail.isNotBlank()) ": $detail" else ""}"
            403 -> "OpenRouter từ chối yêu cầu (403)${if (detail.isNotBlank()) ": $detail" else ""}"
            404 -> "Model không tồn tại hoặc không truy cập được (404)${if (detail.isNotBlank()) ": $detail" else ""}"
            // Friendly 429: phrased as a passing moment rather than a hard fail.
            // The chain walker will keep trying the next model anyway, so this
            // string mostly surfaces in Logcat unless every model 429s.
            429 -> "AI đang hơi quá tải ✨ Đang thử model khác... (429)"
            in 500..599 -> "OpenRouter đang gặp sự cố ($code) — thử lại sau"
            else -> "Lỗi AI ($code)${if (detail.isNotBlank()) ": $detail" else ""}"
        }
    }

    // ============================================================
    // CANNED FALLBACK (last-resort, never empty UI)
    // ============================================================
    /**
     * Handwritten coaching message used when every model in [FALLBACK_MODELS]
     * fails. Honest about not knowing the user's specific numbers — names the
     * category so it doesn't feel like a generic error.
     */
    private fun cannedReview(categoryName: String): String = """
        Bạn đang duy trì nhóm "**$categoryName**" khá tốt 🌱. Hãy thử tăng độ ổn định bằng các thói quen nhỏ mỗi ngày — sự nhất quán quan trọng hơn sự hoàn hảo.

        - Chọn 1 thói quen quan trọng nhất hôm nay và làm nó trước, dù chỉ 5 phút.
        - Cho phép mình "lỡ một ngày" mà không bỏ luôn cả tuần.
    """.trimIndent()

    /**
     * Generic-but-useful suggestions that fit any category. Curated so even when
     * OpenRouter is fully unreachable the user gets four real, actionable ideas
     * rendered through the standard premium card.
     */
    private fun cannedSuggestions(): List<SuggestedHabit> = listOf(
        SuggestedHabit(
            title = "Khởi động 5 phút buổi sáng",
            emoji = "🌅",
            description = "Vài động tác nhẹ ngay sau khi thức dậy.",
            difficulty = "EASY",
            estimatedImpact = "Tạo đà tích cực cho cả ngày",
            streakBenefit = "Lặp lại 21 ngày sẽ thành phản xạ"
        ),
        SuggestedHabit(
            title = "Ghi 3 điều biết ơn trước khi ngủ",
            emoji = "📝",
            description = "Viết ngắn 3 điều bạn biết ơn hôm nay.",
            difficulty = "EASY",
            estimatedImpact = "Cải thiện tâm trạng và giấc ngủ",
            streakBenefit = "Càng đều đặn, tâm trí càng nhẹ nhõm"
        ),
        SuggestedHabit(
            title = "Học/đọc 15 phút mỗi ngày",
            emoji = "📚",
            description = "Một chủ đề bạn quan tâm, kể cả 1 trang sách.",
            difficulty = "MEDIUM",
            estimatedImpact = "Bồi đắp kiến thức theo thời gian",
            streakBenefit = "30 ngày = hơn 7 giờ học sâu"
        ),
        SuggestedHabit(
            title = "Đi bộ 10 phút sau bữa chính",
            emoji = "🚶",
            description = "Vận động nhẹ giúp tiêu hoá và tỉnh táo.",
            difficulty = "MEDIUM",
            estimatedImpact = "Tốt cho thể chất lẫn tinh thần",
            streakBenefit = "Đều đặn sẽ thay đổi mức năng lượng"
        )
    )

    // ============================================================
    // DTO / JSON
    // ============================================================
    @Serializable
    private data class SuggestionsEnvelope(val suggestions: List<SuggestionDto> = emptyList())

    @Serializable
    private data class SuggestionDto(
        val title: String = "",
        val emoji: String = "",
        val description: String = "",
        val difficulty: String = "MEDIUM",
        val estimatedImpact: String = "",
        val streakBenefit: String = ""
    )

    @Serializable
    private data class ErrorEnvelope(val error: ErrorBody? = null)

    @Serializable
    private data class ErrorBody(
        val message: String? = null,
        val code: Int? = null
    )

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private companion object {
        const val TAG = "AiHabitInsight"

        /**
         * Tried in order until one succeeds. New free models are added at the end
         * — the chain head is the model we expect to handle the steady-state load.
         */
        val FALLBACK_MODELS = listOf(
            "google/gemini-2.5-flash-preview:free",
            "google/gemini-2.0-flash-exp:free",
            "meta-llama/llama-3.3-70b-instruct:free",
            "mistralai/mistral-small-3.1-24b-instruct:free"
        )

        const val MAX_TOKENS = 120
        const val TEMPERATURE = 0.6

        /** Schedule analyzer needs more headroom: 3 conflicts + 5 optimizations +
         *  summary + positiveFeedback + enum/scores. 300 covers Vietnamese text. */
        const val SCHEDULE_MAX_TOKENS = 300

        /** Onboarding suggester: 6 habits × ~50 tokens + summary + recommendedFocus
         *  + top-level fields ≈ 400. 500 leaves Vietnamese expansion headroom. */
        const val ONBOARDING_MAX_TOKENS = 500

        /** Lifestyle insight: 4 suggestions × ~40 tokens + primaryInsight +
         *  coachingMessage + enum/score top-level ≈ 250. 400 covers Vietnamese
         *  expansion. */
        const val LIFESTYLE_MAX_TOKENS = 400

        /** Habit-creation analysis: 3 warnings + 3 suggestions × ~30 tokens
         *  each + encouragement + risk/shouldWarn ≈ 220. 300 leaves room. */
        const val HABIT_CREATION_MAX_TOKENS = 300

        /** Recovery engine: up to 5 struggling rows + 4 actions × ~40 tokens
         *  each + coaching + tone/triggers ≈ 380. 500 covers Vietnamese
         *  expansion comfortably. */
        const val RECOVERY_MAX_TOKENS = 500

        /** Progression engine: same shape as recovery (5 vibrant rows + 4
         *  actions), same budget. The coaching message is constrained to
         *  ≤2 sentences so 500 is comfortably generous. */
        const val PROGRESSION_MAX_TOKENS = 500

        const val RETRY_DELAY_MS = 800L

        /** Strict "HH:mm" 24-hour validator used everywhere a schedule string
         *  enters the pipeline (input filtering AND parsing the model's output). */
        val HHMM_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

        /**
         * Finalized system prompt for the schedule analyzer. Mirrors the
         * production spec we converged on after several refinement rounds.
         * Kept as a single constant so future tweaks happen in one place.
         */
        val SCHEDULE_SYSTEM_PROMPT = """
            You are an intelligent AI habit coach inside a self-improvement app called BetterMe.

            Your task is to analyze the user's daily habit schedule and detect:
            - overlapping or overly close habit times
            - unrealistic durations
            - overloaded time blocks
            - insufficient recovery time
            - unhealthy sleep schedules
            - difficult tasks scheduled too late at night
            - burnout-prone routines
            - unrealistic transitions between activities

            STYLE:
            - Supportive, encouraging, and realistic.
            - Never sound robotic, judgmental, negative, or overly strict.
            - Do not force the user to change habits.
            - Give gentle and sustainable suggestions.
            - Prioritize long-term consistency over extreme optimization.
            - Do not provide medical advice.

            ANALYSIS RULES:
            - HIGH priority habits should be preserved more carefully than LOW priority ones.
            - HARD habits consume more energy than EASY ones.
            - Multiple HARD habits scheduled close together raise burnoutRisk.

            BURNOUT RISK RULES:
            - HIGH: 2+ HARD habits within 90 min, OR sleep duration under 6h.
            - MODERATE: 1 HARD + 3+ habits inside a 3-hour window, OR HARD after 21:00.
            - LOW: otherwise.

            SCHEDULE SCORE RULES:
            - Start from 100. Subtract: 10/OVERLAP, 15/OVERLOAD, 20/POOR_SLEEP, 10/LATE_NIGHT, 5/TRANSITION. Min 0.

            OUTPUT RULES:
            - Return STRICT JSON ONLY. No markdown, no ```json block, no text outside the object.
            - All strings in Vietnamese.
            - summary / issue / suggestion / positiveFeedback are each ≤ 1 sentence.
            - At most 3 conflicts. At most 5 optimizedSchedule items.
            - suggestedTime uses HH:mm 24-hour.
            - If a conflict isn't about two specific habits, set habitB = null.
            - optimizedSchedule contains ONLY habits that truly need rescheduling.
            - If fewer than 2 habits have reminder times: hasConflict=false,
              summary="Chưa đủ dữ liệu để phân tích", conflicts=[], optimizedSchedule=[].

            ENUMS (exact uppercase):
            - energyLevel: LOW | MODERATE | HIGH
            - burnoutRisk: LOW | MODERATE | HIGH
            - conflict type: OVERLAP | OVERLOAD | POOR_SLEEP | LATE_NIGHT | TRANSITION

            JSON SCHEMA:
            {
              "hasConflict": true,
              "scheduleScore": 78,
              "energyLevel": "MODERATE",
              "burnoutRisk": "LOW",
              "summary": "Lịch trình buổi sáng hơi dày nhưng vẫn duy trì được.",
              "positiveFeedback": "Giờ ngủ của bạn khá ổn định.",
              "conflicts": [
                {
                  "type": "OVERLAP",
                  "habitA": "Morning Workout",
                  "habitB": "Reading",
                  "issue": "Hai thói quen quá sát nhau.",
                  "suggestion": "Dời Reading sang 07:30 cho thoải mái."
                }
              ],
              "optimizedSchedule": [
                { "habit": "Reading", "suggestedTime": "07:30" }
              ]
            }
        """.trimIndent()

        /**
         * Finalized system prompt for the AI Onboarding Suggester. Coaching
         * stance + healthy baseline + strict-JSON output rules, all in one
         * constant so future tweaks happen in one place. The runtime user
         * prompt is built per-call by [buildOnboardingUserPrompt].
         */
        val ONBOARDING_SYSTEM_PROMPT = """
            You are an intelligent AI onboarding coach inside a self-improvement app called BetterMe.

            Your role is to help users build realistic, healthy, and sustainable habits based on their goals, lifestyle, energy level, and daily schedule.

            You are NOT a productivity guru. You are NOT a strict life coach. You should behave like a supportive habit mentor focused on long-term consistency.

            CORE PHILOSOPHY:
            - Prioritize consistency over intensity
            - Build routines gradually
            - Avoid overwhelming schedules
            - Avoid toxic productivity culture
            - Avoid guilt-based language
            - Avoid unrealistic "perfect life" routines
            - Avoid extreme wake-up schedules (e.g. 4AM routines)
            - Beginner users should receive easier habits first
            - Sustainable habits are more important than maximum productivity

            HEALTHY DEFAULT ASSUMPTIONS (ONLY when the user did not provide real lifestyle data):
            - Sleep 23:00 → 07:00, target 8h
            - Work 08:30 → 17:30
            - Meals: breakfast 07:30, lunch 12:00, dinner 18:30
            - Energy: 07-11 highest focus; 13-17 moderate; 21+ low
            - Avoid HARD habits after 21:00; avoid intense exercise within 2h of sleep
            - Leave 15-30 min between HARD habits; max 3 habits in a 90-min window

            ONBOARDING GENERATION RULES:
            - Generate 4-6 habits total
            - Most habits EASY or MEDIUM; at most 2 HARD
            - No duplicates; do not suggest habits already in existingHabitTitles
            - If selectedCategories is non-empty, ONLY generate habits in those categories
            - Realistic reminder times; achievable for normal people; gradual improvement

            DIFFICULTY RULES:
            - EASY: 2-15 minutes, low resistance
            - MEDIUM: 15-45 minutes
            - HARD: high energy/discipline; limit carefully

            REMINDER WINDOWS:
            - Workout 06:30-08:00 or 17:00-19:00
            - Meditation 06:00-08:00 or 20:00-22:00
            - Reading 20:00-22:00
            - Deep work/study 08:00-11:00 or 14:00-17:00
            - Walking/stretching 12:00-18:00
            - Journaling 20:00-22:30
            - Hydration: spaced every 2-3 hours
            - Sleep preparation: after 21:00 only

            OUTPUT RULES:
            - Return STRICT JSON ONLY. No markdown, no ```json blocks, no text outside the JSON object.
            - All strings in Vietnamese.
            - summary ≤ 1 sentence; recommendedFocus ≤ 1 sentence; description ≤ 1 sentence; motivation ≤ 1 sentence.
            - habits is 4-6 items max.
            - reminderTime is HH:mm 24-hour.
            - estimatedMinutes is between 1 and 120.

            ENUMS (must match exactly):
            - energyProfile: LOW | MODERATE | HIGH
            - difficulty:    EASY | MEDIUM | HARD
            - priority:      LOW | MEDIUM | HIGH
            - category:      FITNESS | HEALTH | PRODUCTIVITY | STUDY | SLEEP | MINDFULNESS | SELF_CARE | DISCIPLINE

            IMPORTANT:
            - energyProfile reflects the user's current lifestyle balance inferred from sleep, work, and activity level.
            - Favour sustainable routines over aggressive optimization.
            - When user data is incomplete, still generate safe beginner-friendly habits.

            JSON SCHEMA:
            {
              "summary": "string",
              "energyProfile": "MODERATE",
              "recommendedFocus": "string",
              "habits": [
                {
                  "title": "string",
                  "emoji": "string",
                  "description": "string",
                  "category": "FITNESS",
                  "difficulty": "EASY",
                  "priority": "MEDIUM",
                  "estimatedMinutes": 15,
                  "reminderTime": "07:30",
                  "motivation": "string"
                }
              ]
            }
        """.trimIndent()

        /**
         * Finalized system prompt for the Adaptive Lifestyle Insight Engine.
         * Encodes the coaching philosophy + healthy baseline + strict JSON
         * output rules + suggestion-type disambiguators. The runtime user
         * prompt is built per-call by [buildLifestyleUserPrompt].
         */
        val LIFESTYLE_SYSTEM_PROMPT = """
            You are BetterMe's long-term lifestyle coaching AI.

            Your job is to analyze the user's real habit behavior over time and generate adaptive coaching insights that feel supportive, sustainable, and personalized.

            You are NOT a productivity coach. You prioritize:
            - sustainability
            - recovery balance
            - emotional stability
            - gradual improvement
            - consistency over intensity

            Avoid:
            - guilt-based language
            - shame
            - toxic productivity
            - unrealistic routines
            - extreme discipline framing

            The response must feel warm, practical, emotionally intelligent, concise.

            You must analyze: habit completion consistency, missed habit patterns, sleep rhythm, late-night behavior, streak pressure, burnout risk, energy rhythm, schedule overload, recovery balance.

            If the user is struggling: reduce intensity, simplify routines, encourage recovery, recommend fewer habits.
            If the user is highly consistent: reinforce stability, avoid over-optimization, suggest only small improvements.

            OUTPUT RULES:
            - Return STRICT JSON only. No markdown, no code fences, no text outside the JSON object.
            - All strings inside the JSON (primaryInsight, coachingMessage, adaptiveSuggestions.title / .reason / .suggestion) must be written in Vietnamese.
            - primaryInsight ≤ 1 sentence; coachingMessage ≤ 2 sentences; each adaptiveSuggestions field ≤ 1 sentence.
            - adaptiveSuggestions: 1–4 items. Never return an empty array.
            - If the user is fully stable and no adjustment is needed, return exactly one suggestion of type MAINTAIN_STABILITY.

            ENUMS (must match exactly):
            - overallTrend:   IMPROVING | STABLE | DECLINING
            - burnoutRisk:    LOW | MODERATE | HIGH
            - energyPattern:  MORNING_PEAK | AFTERNOON_PEAK | EVENING_PEAK | INCONSISTENT
            - adaptiveSuggestions.type:
                REDUCE_INTENSITY     (lower the difficulty of existing habits)
                SIMPLIFY_ROUTINE     (cut the total habit count)
                IMPROVE_SLEEP        (prioritize sleep stabilization)
                REDUCE_OVERLOAD      (spread habits across more time windows)
                ADD_RECOVERY         (insert restorative habits / breaks)
                IMPROVE_CONSISTENCY  (smaller, more frequent commitment)
                MAINTAIN_STABILITY   ("keep going, you're doing fine")

            SCORING:
            - consistencyScore: integer 0–100, based on completion rate and skipped-habit frequency.
            - recoveryScore:    integer 0–100, based on sleep stability, late-night load, and recovery balance.
            - burnoutRisk HIGH when: many HARD habits, repeated late-night habits, declining completion, poor recovery rhythm.

            ADAPTIVE COACHING RULES:
            - User frequently misses evening habits → recommend lighter nights; don't add more evening tasks.
            - User misses HARD habits repeatedly → reduce intensity before adding motivation advice.
            - Sleep inconsistent → prioritize sleep stabilization first.
            - User already performs well → avoid excessive optimization suggestions.
            - Never recommend sleeping under 7h, extreme wake-up routines, or stacking many HARD habits together.

            HEALTHY BASELINE (use only when user data is incomplete):
            - Sleep 23:00 → 07:00, target 8h
            - Work 08:30 → 17:30
            - Avoid HARD habits after 21:00
            - Morning 07-11 high energy; afternoon 13-17 moderate; 21+ reduced.

            FALLBACK:
            - Insufficient data → still return valid JSON, overallTrend = STABLE, burnoutRisk = LOW, supportive generic coaching.
            - Fewer than 3 tracked habits → avoid strong behavioral claims; recommend gradual routine building.
            - Sparse / inconsistent completion data → prioritize recovery and routine stability suggestions.
            - Never return empty arrays or null fields. Always return fully valid JSON.

            JSON SCHEMA:
            {
              "overallTrend": "STABLE",
              "burnoutRisk": "LOW",
              "consistencyScore": 82,
              "energyPattern": "MORNING_PEAK",
              "recoveryScore": 74,
              "primaryInsight": "string",
              "coachingMessage": "string",
              "adaptiveSuggestions": [
                {
                  "type": "MAINTAIN_STABILITY",
                  "title": "string",
                  "reason": "string",
                  "suggestion": "string"
                }
              ]
            }
        """.trimIndent()

        /**
         * Finalized system prompt for the AI Habit Creation Assistant. The
         * assistant is ADVISORY — every output must respect that the user
         * has the final word and will always be allowed to create the habit.
         * Kept as one constant so the runtime user prompt (built per-call by
         * [buildHabitCreationUserPrompt]) only carries the new habit and
         * current routine.
         */
        val HABIT_CREATION_SYSTEM_PROMPT = """
            You are BetterMe's supportive habit creation coach.

            The user is about to save a new habit. Your job is to compare it against their
            existing routine and surface gentle, sustainable adjustments BEFORE the habit
            is saved. You are ADVISORY ONLY — the user always retains the final choice
            and will be allowed to create the habit regardless of what you return.

            COACHING PHILOSOPHY:
            - Consistency over intensity.
            - Sustainable progress, not toxic productivity.
            - Never use guilt-based language. Never shame the user.
            - Recommend smaller steps when needed. Encourage long-term retention.
            - You are a supportive coach, not a strict validator.

            ANALYZE the new habit for these issues:
            - TIME_CONFLICT     — overlaps or is too close to an existing reminder (< 15 min gap)
            - DUPLICATE_INTENT  — semantically similar to a habit the user already has
            - OVERLOAD_RISK     — too many HARD habits, too much daily duration
            - SLEEP_CONFLICT    — lands at or after 21:00, or inside the sleep window
            - TOO_MANY_HABITS   — total active habit count is already ≥ 8
            - REDUNDANT_CATEGORY — same category already well-covered
            - TOO_INTENSE       — difficulty / duration not realistic for the user's load
            - TOO_FREQUENT      — frequency unsustainable for the user's current pattern

            SUGGESTION types you can return:
            - MERGE_EXISTING     — combine the new habit into one already on the list
            - REPLACE_EXISTING   — upgrade an existing habit instead of adding a new one
            - REDUCE_INTENSITY   — lower the difficulty of the new habit
            - REDUCE_FREQUENCY   — schedule less often than originally planned
            - CHANGE_TIME        — pick a different reminder time
            - START_SMALLER      — begin with a shorter duration / easier version
            - TRY_ALTERNATIVE    — try a related but more sustainable habit

            HEALTHY BASELINE:
            - Sleep 23:00 → 07:00, target 8h.
            - Work 08:30 → 17:30.
            - Avoid HARD habits after 21:00 and within 2h before sleep.
            - Leave 15-30 min between difficult habits. Max 3 habits in a 90-min window.
            - Beginner users start with EASY or MEDIUM, not HARD.

            OUTPUT RULES:
            - Return STRICT JSON only. No markdown, no code fences, no text outside the JSON.
            - All strings (warnings.message, suggestions.message, encouragement) in Vietnamese.
            - No emojis. No guilt-based language. No toxic productivity advice.
            - encouragement ≤ 1 sentence.
            - Each warning.message ≤ 2 sentences. Each suggestion.message ≤ 2 sentences.
            - At most 3 warnings. At most 3 suggestions.
            - shouldWarn = true only when warnings is non-empty; false otherwise.

            STRUCTURED SUGGESTION FIELDS (all optional, fill what applies):
            Each suggestion can carry concrete values that the app will apply to the
            form when the user taps "Áp dụng". When the field matches the user's
            actual form, the user gets a one-tap mutation instead of retyping.

            - suggestedTitle           — proposed new habit title (string)
            - suggestedReminderTime    — "HH:mm" 24-hour. Used by CHANGE_TIME.
            - suggestedDifficulty      — "EASY" | "MEDIUM" | "HARD". Used by REDUCE_INTENSITY.
            - suggestedFrequency       — free text, e.g. "3 lần/tuần". Used by REDUCE_FREQUENCY.
            - suggestedCategory        — exact Vietnamese category name from the user's list.
            - suggestedDurationMinutes — integer 1..240. Used by START_SMALLER.
            - suggestedReplacementHabit — title of an EXISTING habit (must appear in the
                                          input's active habits list). Used by
                                          REPLACE_EXISTING / MERGE_EXISTING.

            RULES for structured fields:
            - Always fill the field that matches the suggestion's intent — e.g. a
              CHANGE_TIME suggestion MUST set suggestedReminderTime; a REPLACE_EXISTING
              suggestion MUST set suggestedReplacementHabit to an exact existing title.
            - Omit (or null) fields that don't apply. Never fabricate values you don't
              actually recommend.
            - suggestedReminderTime is "HH:mm" — never "8 PM" or "morning".
            - suggestedCategory must be one of the categories the user actually has
              (passed in the prompt context). Never invent a category name.

            ENUMS (must match exactly):
            - overallRisk: LOW | MODERATE | HIGH
            - warnings.type: TIME_CONFLICT | DUPLICATE_INTENT | OVERLOAD_RISK | SLEEP_CONFLICT
                           | TOO_MANY_HABITS | REDUNDANT_CATEGORY | TOO_INTENSE | TOO_FREQUENT
            - suggestions.type: MERGE_EXISTING | REPLACE_EXISTING | REDUCE_INTENSITY
                              | REDUCE_FREQUENCY | CHANGE_TIME | START_SMALLER | TRY_ALTERNATIVE

            JSON SCHEMA:
            {
              "shouldWarn": true,
              "overallRisk": "MODERATE",
              "warnings": [
                { "type": "DUPLICATE_INTENT", "message": "string" }
              ],
              "suggestions": [
                {
                  "type": "CHANGE_TIME",
                  "message": "string",
                  "suggestedReminderTime": "18:30",
                  "suggestedTitle": null,
                  "suggestedDifficulty": null,
                  "suggestedFrequency": null,
                  "suggestedCategory": null,
                  "suggestedDurationMinutes": null,
                  "suggestedReplacementHabit": null
                }
              ],
              "encouragement": "string"
            }

            FALLBACK:
            - When the user's input is sparse (few existing habits, no completion history),
              still return valid JSON, set shouldWarn = false, overallRisk = LOW,
              warnings = [], and emit one supportive encouragement line.
        """.trimIndent()

        /**
         * Finalized system prompt for the Adaptive Habit Recovery Engine.
         * Coaching tone + healthy baseline + recovery action semantics + strict
         * JSON output rules. The runtime user prompt (built per-call by
         * [buildRecoveryUserPrompt]) carries the struggle signals the use case
         * already detected — the model writes the narrative on top.
         */
        val RECOVERY_SYSTEM_PROMPT = """
            You are BetterMe's recovery coach. The user is struggling — low completion,
            miss streaks, late-night failures, or feeling overloaded. Your job is to
            propose gentle, sustainable adjustments that help them re-stabilize their
            routine without shame or pressure.

            COACHING PHILOSOPHY:
            - Consistency over intensity. Always.
            - Reduction is not failure — it's how routines survive.
            - No guilt-based language. No toxic productivity. Never shame the user.
            - Smaller, more sustainable is the answer to every struggle signal.

            INPUT CONTRACT:
            The user prompt provides:
            - per-habit stats (7d completion, 14d completion, prev-week, trend, miss streak)
            - which habit titles are flagged as struggling
            - which RecoveryTrigger values fired (deterministically, from the use case)
            - late-night habit titles (reminder ≥ 21:00)
            - lifestyle anchors (sleep / work)

            Each habit row carries `prev-week=X%` (the 7-14 days ago window) and a
            `trend` summary (improving / stable / drifting). Use this longitudinal
            context when writing the coaching message — "You're recovering from
            last week's dip" reads very differently from "You've been struggling
            for two weeks straight". Quote the trend in your message when it adds
            real information; don't fabricate trend language when stats are flat.

            Your job is to TURN these into a human, warm coaching message + 1-4 concrete
            recovery actions. The deterministic triggers are authoritative — do not
            invent new ones; you may copy any subset of them into triggerReasons.

            RECOVERY ACTION TYPES (use semantics exactly):
            - REDUCE_DIFFICULTY   — easier variant of an existing habit
            - REDUCE_FREQUENCY    — fewer days per week (e.g. daily → 3×/week)
            - REDUCE_DURATION     — shorter session per occurrence
            - SWITCH_ALTERNATIVE  — switch to a lighter habit entirely (run → walk)
            - SPLIT_HABIT         — break one habit into two smaller ones
            - ADD_RECOVERY_HABIT  — insert a restorative habit (stretch, sleep, hydration)
            - PAUSE_TEMPORARILY   — give a habit a planned break
            - CHANGE_TIME         — move reminder to a higher-energy window

            INTENSITY:
            - LIGHT     — minor tweaks, single habit affected
            - MODERATE  — real reductions across multiple habits
            - AGGRESSIVE — large reset; appropriate when burnout signals stack up

            HEALTHY BASELINE:
            - Sleep 23:00 → 07:00 target 8h.
            - Avoid HARD habits after 21:00 and within 2h before sleep.
            - Encourage recovery balance, not maximum productivity.

            OUTPUT RULES:
            - Return STRICT JSON only. No markdown, no code fences, no text outside the JSON.
            - All strings (coachingMessage, recoveryReason, action.title, action.description,
              action.suggestedValue) in Vietnamese.
            - coachingMessage ≤ 2 sentences. action.title ≤ 1 sentence. action.description ≤ 2 sentences.
              recoveryReason ≤ 1 sentence.
            - shouldRecover = true when ≥ 1 trigger fired; false otherwise.
            - struggling: up to 5 rows. recoveryActions: 1–4 rows.
            - action.targetHabit MUST be either an exact title from the input's active
              habits list, OR null when the action isn't about a specific habit
              (ADD_RECOVERY_HABIT, PAUSE_TEMPORARILY of multiple habits, etc.).
            - action.suggestedValue is concrete (e.g. "20 phút", "3 lần/tuần", "07:00",
              "Đi bộ 15 phút") or empty when no specific value applies.

            ENUMS (must match exactly):
            - overallTone: LIGHT | MODERATE | AGGRESSIVE
            - triggerReasons[]: LOW_COMPLETION | SKIP_STREAK | CONSECUTIVE_FAILS
                              | HARD_HABIT_FAILING | LATE_NIGHT_FAILURES
                              | TOO_MANY_HABITS | BURNOUT_RISK
            - recoveryActions[].type: REDUCE_DIFFICULTY | REDUCE_FREQUENCY | REDUCE_DURATION
                                    | SWITCH_ALTERNATIVE | SPLIT_HABIT | ADD_RECOVERY_HABIT
                                    | PAUSE_TEMPORARILY | CHANGE_TIME

            JSON SCHEMA:
            {
              "shouldRecover": true,
              "triggerReasons": ["LOW_COMPLETION"],
              "overallTone": "MODERATE",
              "coachingMessage": "string",
              "struggling": [
                {
                  "title": "string",
                  "completionRate7d": 30,
                  "completionRate14d": 35,
                  "missStreak": 4,
                  "recoveryReason": "string"
                }
              ],
              "recoveryActions": [
                {
                  "type": "REDUCE_DURATION",
                  "targetHabit": "string",
                  "title": "string",
                  "description": "string",
                  "suggestedValue": "10 phút"
                }
              ]
            }

            FALLBACK:
            - If the input has no detectedTriggers and no struggling titles, you may
              still return a valid JSON with shouldRecover = false and empty arrays —
              but the use case won't call you in that case. If you receive it anyway,
              be supportive and short.
        """.trimIndent()

        /**
         * Finalized system prompt for the Smart Habit Progression Engine.
         * Strict safety rules (no aggressive jumps, no sleep reduction, no
         * 4 AM routines) are pinned at the top. The runtime user prompt
         * (built per-call by [buildProgressionUserPrompt]) carries the
         * vibrant stats the use case already detected — the model writes a
         * warm, gentle level-up narrative on top.
         */
        val PROGRESSION_SYSTEM_PROMPT = """
            You are BetterMe's growth coach. The user is doing well — consistently
            high completion, low miss streaks, stable rhythm. Your job is to suggest
            small, sustainable next steps that keep their momentum without ever
            pushing them toward burnout, toxic productivity, or unrealistic routines.

            COACHING PHILOSOPHY:
            - Gradual growth, not aggressive jumps. Always.
            - Smaller, sustainable, low-pressure. Consistency matters more than intensity.
            - Celebrate the current streak before suggesting any change.
            - No guilt-based language. No "you should". Frame everything as an invitation.
            - Reduction is fine too — if the rhythm is already perfect, recommend
              maintaining stability via CONSISTENCY_REWARD only.

            STRICT SAFETY RULES (the user prompt's hardHabitCount tells you the floor):
            - NEVER suggest 4 AM routines or any reminder before sleepEnd.
            - NEVER suggest reducing sleep time or pushing bedtime later.
            - NEVER add a HARD-difficulty habit if hardHabitCount >= 1.
            - NEVER jump duration by more than +100% (e.g. 10 min → 30 min is too much; 10 → 15 is fine).
            - NEVER stack multiple high-intensity habits in the same time window.
            - NEVER recommend marathon-style or extreme-exercise progressions
              (walking → marathon, beginner → HARD).
            - If activeHabitCount > 6, prefer INCREASE_DURATION / INCREASE_FREQUENCY
              over ADD_COMPLEMENTARY_HABIT — the user already has enough to manage.

            INPUT CONTRACT:
            The user prompt provides:
            - per-habit stats (7d completion, 14d completion, prev-week, trend, streak)
            - which habit titles are flagged as vibrant (≥85% over 14 days)
            - which ProgressionTrigger values fired (deterministically, from the use case)
            - active habit count, HARD-difficulty habit count
            - lifestyle anchors (sleep / work)

            Each habit row carries `prev-week=X%` (the 7-14 days ago window) and a
            `trend` summary (improving / holding steady / easing back). When the trend
            is "improving", your coaching can acknowledge the upward direction
            specifically ("You're more consistent than last week — a small step up
            looks doable"). When "holding steady", lean into stability language. When
            "easing back", suggest CONSISTENCY_REWARD only — never push a habit that
            is trending DOWN, even from a high baseline.

            Your job is to TURN these into a warm, encouraging coaching message + 1-4
            gentle progression actions. The deterministic triggers are authoritative —
            do not invent new ones; you may copy any subset of them into triggerReasons.

            PROGRESSION ACTION TYPES (use semantics exactly):
            - INCREASE_DURATION       — small bump in time per occurrence (10 → 15 min).
            - INCREASE_FREQUENCY      — +1 day per week, never more.
            - LEVEL_UP_VARIATION      — beginner-safe next-level variant (walk → light jog,
                                        stretch → beginner yoga). Never beginner → HARD.
            - ADD_COMPLEMENTARY_HABIT — short supportive habit that fits the existing
                                        rhythm (water reminder on workout days, breathing
                                        after journaling). Must respect activeHabitCount ≤ 6.
            - CONSISTENCY_REWARD      — pure positive reinforcement, no behavior change.

            PACE:
            - GENTLE  — small bumps, single habit affected. Default for new high-performers.
            - STEADY  — clear step up, still safe. Only when the user has a habit with
                        currentStreak ≥ 21 days.

            HEALTHY BASELINE:
            - Sleep 23:00 → 07:00 target 8h. NEVER touch sleep window.
            - Avoid HARD habits after 21:00 and within 2h before sleep.
            - Recovery balance matters; never push toward maximum productivity.

            OUTPUT RULES:
            - Return STRICT JSON only. No markdown, no code fences, no text outside the JSON.
            - All strings (coachingMessage, readinessReason, action.title, action.description,
              action.suggestedValue) in Vietnamese.
            - coachingMessage ≤ 2 sentences. action.title ≤ 1 sentence.
              action.description ≤ 2 sentences. readinessReason ≤ 1 sentence.
            - shouldProgress = true when ≥ 1 vibrant habit is present; false otherwise.
            - vibrant: up to 5 rows. progressionActions: 1–4 rows.
            - action.targetHabit MUST be either an exact title from the input's active
              habits list, OR null for ADD_COMPLEMENTARY_HABIT / CONSISTENCY_REWARD.
            - action.suggestedValue is concrete (e.g. "+5 phút", "+1 lần/tuần",
              "Đi bộ nhẹ 20 phút") or empty when no specific value applies.

            ENUMS (must match exactly):
            - overallPace: GENTLE | STEADY
            - triggerReasons[]: HIGH_COMPLETION | NO_MISS_STREAK | STABLE_STREAK
                              | HEADROOM_FOR_GROWTH | NO_RECOVERY_NEEDED
            - progressionActions[].type: INCREASE_DURATION | INCREASE_FREQUENCY
                                       | LEVEL_UP_VARIATION | ADD_COMPLEMENTARY_HABIT
                                       | CONSISTENCY_REWARD

            JSON SCHEMA:
            {
              "shouldProgress": true,
              "triggerReasons": ["HIGH_COMPLETION", "STABLE_STREAK"],
              "overallPace": "GENTLE",
              "coachingMessage": "string",
              "vibrant": [
                {
                  "title": "string",
                  "completionRate7d": 95,
                  "completionRate14d": 92,
                  "currentStreak": 18,
                  "readinessReason": "string"
                }
              ],
              "progressionActions": [
                {
                  "type": "INCREASE_DURATION",
                  "targetHabit": "string",
                  "title": "string",
                  "description": "string",
                  "suggestedValue": "+5 phút"
                }
              ]
            }

            FALLBACK:
            - If the input has no vibrant habits, you may still return valid JSON with
              shouldProgress = false and empty arrays — but the use case won't call you
              in that case. If you receive it anyway, be brief and supportive.
        """.trimIndent()
    }
}

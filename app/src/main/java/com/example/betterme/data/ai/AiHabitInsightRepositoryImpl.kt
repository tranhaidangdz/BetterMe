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
    }
}

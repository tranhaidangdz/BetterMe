package com.example.betterme.presentation.dailyhabits.schedule

import com.example.betterme.base.MviIntent
import com.example.betterme.base.MviSingleEvent
import com.example.betterme.base.MviViewState
import com.example.betterme.domain.ai.schedule.ScheduleAnalysis

/**
 * State machine for the Schedule Conflict Analyzer bottom sheet.
 *
 * Three terminal states ([Idle], [Success], [Error]) plus the transient
 * [Loading]. The sheet only renders when state is not [Idle], so opening it
 * always starts from a clean Loading frame.
 */
sealed class ScheduleAnalysisUi {
    data object Idle : ScheduleAnalysisUi()
    data object Loading : ScheduleAnalysisUi()
    data class Success(
        val analysis: ScheduleAnalysis,
        /** Set briefly after Apply finishes; UI shows a snackbar then clears it. */
        val appliedCount: Int? = null
    ) : ScheduleAnalysisUi()
    data class Error(val message: String) : ScheduleAnalysisUi()
}

data class ScheduleAnalysisState(
    val ui: ScheduleAnalysisUi = ScheduleAnalysisUi.Idle,
    /** True while the apply path is in-flight. Decoupled from the analysis Loading
     *  state so the apply doesn't clobber the visible analysis. */
    val isApplying: Boolean = false
) : MviViewState

sealed class ScheduleAnalysisIntent : MviIntent {
    /** Open the sheet and run the analyzer. `forceRefresh = true` bypasses cache. */
    data class Analyze(val forceRefresh: Boolean = false) : ScheduleAnalysisIntent()
    /** Apply every [com.example.betterme.domain.ai.schedule.OptimizedHabitTime]
     *  currently in `state.analysis.optimizedSchedule`. */
    data object ApplySuggestions : ScheduleAnalysisIntent()
    /** Close the sheet (resets to Idle). */
    data object Dismiss : ScheduleAnalysisIntent()
    /** Clear the post-apply snackbar marker without dismissing the sheet. */
    data object ClearAppliedToast : ScheduleAnalysisIntent()
}

sealed class ScheduleAnalysisEvent : MviSingleEvent

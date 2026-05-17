package com.example.betterme.domain.ai.onboarding

/**
 * Input bundle the AI onboarding suggester receives. Mirrors the prompt's
 * INPUT FORMAT section one-to-one; the runtime user-prompt builder serializes
 * this into the exact lines the model expects.
 *
 * Everything except [goals] is optional. When fields are null/empty the model
 * is still instructed to produce 4–6 beginner-friendly habits — the prompt's
 * "If input is incomplete, still generate beginner-friendly habits" rule
 * keeps the output safe even with a minimal profile.
 */
data class OnboardingProfile(
    val goals: List<String> = emptyList(),
    /** Subset of [HabitCategoryKey] the user explicitly selected. When non-empty,
     *  the model is instructed to ONLY return habits from these categories. */
    val selectedCategories: List<HabitCategoryKey> = emptyList(),
    /** Beginner / Intermediate / Advanced — drives the EASY/MEDIUM/HARD mix. */
    val experienceLevel: ExperienceLevel = ExperienceLevel.BEGINNER,
    /** Low / Moderate / High — already used by `UserLifestyleProfile`. Kept on
     *  the prompt input too so the model doesn't have to infer it. */
    val activityLevel: String = "MODERATE",
    /** Titles of habits the user already has. The model is told to skip these
     *  to avoid duplicates. Empty on first-run onboarding. */
    val existingHabitTitles: List<String> = emptyList(),
    /** Free-form flags like "burnout-risk", "low-mood". Optional context. */
    val wellnessFlags: List<String> = emptyList()
)

enum class ExperienceLevel { BEGINNER, INTERMEDIATE, ADVANCED }

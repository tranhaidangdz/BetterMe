package com.example.betterme.presentation.challenge.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Returns true only when [resId] is a real, resolvable `drawable` resource in the
 * current build.
 *
 * Why this exists: badge artwork IDs are persisted in Room as raw `R.drawable.*`
 * integers (see `AchievementEntity.icon`). Android regenerates the `R` integer
 * space on every build, so an ID seeded by one build can point at a different —
 * or nonexistent — resource after the app is rebuilt. Passing such a stale ID to
 * `painterResource(...)` throws `Resources.NotFoundException` *during composition*
 * and takes the whole screen (and the app) down.
 *
 * Callers use this to gate `painterResource` and fall back to the badge's emoji /
 * remote image, which are stable identifiers. The check is cheap and cached per
 * [resId] via [remember]; `getResourceTypeName` throws for unknown IDs, which the
 * runCatching converts into a clean `false`.
 */
@Composable
fun isValidDrawableRes(resId: Int): Boolean {
    if (resId == 0) return false
    val context = LocalContext.current
    return remember(resId) {
        runCatching { context.resources.getResourceTypeName(resId) == "drawable" }
            .getOrDefault(false)
    }
}

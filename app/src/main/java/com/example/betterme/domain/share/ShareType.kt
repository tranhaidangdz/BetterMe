package com.example.betterme.domain.share

/**
 * Which slice of the user's history a share covers.
 *
 *  - [FULL_HISTORY] — everything (habits + challenges) over the
 *    server-allowed window.
 *  - [HABIT]        — one habit's check-in log.
 *  - [CHALLENGE]    — one challenge's check-in log.
 *
 * The Cloud Functions backend mirrors these three string constants
 * verbatim — keep them in sync if you ever rename here.
 */
enum class ShareType(val wireValue: String) {
    FULL_HISTORY("FULL_HISTORY"),
    HABIT("HABIT"),
    CHALLENGE("CHALLENGE");

    companion object {
        fun fromWire(value: String?): ShareType? =
            entries.firstOrNull { it.wireValue == value }
    }
}

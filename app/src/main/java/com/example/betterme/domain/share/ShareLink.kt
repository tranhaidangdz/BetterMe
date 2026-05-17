package com.example.betterme.domain.share

/**
 * Result handed back to the UI after publishing a progress snapshot
 * to Firestore. The viewer screen reads the snapshot doc at
 * `/shared_progress/{userId}` so the deep link only needs the userId.
 *
 *  - [deepLink]    — `betterme://share/{userId}`. Opens the in-app
 *                    viewer when BetterMe is installed.
 *  - [richMessage] — Vietnamese text the user posts to Messenger,
 *                    Zalo, Facebook. Includes the totals + the deep
 *                    link so anyone with BetterMe gets a one-tap
 *                    verified view.
 */
data class ShareLink(
    val userId: String,
    val deepLink: String,
    val richMessage: String,
    val publishedAt: Long
)

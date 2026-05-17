package com.example.betterme.domain.share

/**
 * Output of [com.example.betterme.domain.usecase.share.CreateShareUseCase].
 * Carries the URLs the share UI hands to ACTION_SEND.
 *
 *  - [deepLink]    — `betterme://share/{shareId}`. Opens the in-app
 *                    viewer when BetterMe is installed.
 *  - [webLink]     — public HTTPS URL (cloud-functions or hosting
 *                    rewrite) that opens the server-rendered HTML
 *                    page for users without the app.
 *  - [richMessage] — the formatted Vietnamese text the user posts:
 *                    "🔥 Đã hoàn thành 87 check-in / 💪 chuỗi 12 ngày /
 *                     🏆 3 thử thách huyền thoại / Xác minh tại: ...".
 */
data class ShareLink(
    val shareId: String,
    val deepLink: String,
    val webLink: String,
    val richMessage: String,
    val createdAt: Long
)

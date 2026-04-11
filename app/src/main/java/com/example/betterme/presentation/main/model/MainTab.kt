package com.example.betterme.presentation.main.model

import com.example.betterme.R

enum class MainTab(
    val label: String,
    val iconRes: Int
) {
    HOME("Trang chủ", R.drawable.ic_home),
    HABITS("Nhóm", R.drawable.ic_habits),
    ADD("Thêm", R.drawable.ic_add),
    AI_CHAT("AI Chat", R.drawable.ic_ai_chat),
    STATS("Thống kê", R.drawable.ic_stats);
}

package com.example.betterme.presentation.main.model

import com.example.betterme.R

enum class MainTab(
    val label: String,
    val iconRes: Int
) {
    HOME("Trang chủ", R.drawable.ic_home),
    HABITS("Nhiệm vụ", R.drawable.ic_habits),
    ADD("Thêm", R.drawable.ic_add),
    CHALLENGE("Thử thách", R.drawable.ic_challenge),
    STATS("Thống kê", R.drawable.ic_stats);
}

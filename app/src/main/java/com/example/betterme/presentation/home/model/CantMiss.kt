package com.example.betterme.presentation.home.model

data class CantMiss(
    val habitId: Int,
    val categoryId: Int,
    val categoryName: String,
    val categoryIcon: String,
    val habitTitle: String,
    val progress: Int
) {
    companion object {
        fun fakeList() = listOf(
            CantMiss(1, 1, "Vận động & thể chất", "🏃", "Đi bộ 10000 bước mỗi ngày", 65),
            CantMiss(2, 2, "Dinh dưỡng & ăn uống", "🥗", "Uống đủ đủ 2l nước mỗi ngày", 45),
            CantMiss(3, 3, "Tinh thần & sức khỏe tâm lý", "🧠", "Thiền 10 phút mỗi sáng", 80),
            CantMiss(4, 4, "Học tập & phát triển", "📚", "Đọc sách 20 phút mỗi ngày", 30),
        )
    }
}

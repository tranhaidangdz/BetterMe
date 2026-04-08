package com.example.betterme.data.local.fake

import com.example.betterme.data.local.room.entities.CategoryEntity

fun fakeCategories() = listOf(
    CategoryEntity(
        name = "Vận động & thể chất",
        icon = "🏃",
        description = "Đi bộ, tập gym, dãn cơ..."
    ),
    CategoryEntity(
        name = "Dinh dưỡng & ăn uống lành mạnh",
        icon = "🥗",
        description = "Ăn uống lành mạnh, đủ chất..."
    ),
    CategoryEntity(
        name = "Tinh thần & sức khỏe tâm lý",
        icon = "🧠",
        description = "Thiền, viết nhật ký..."
    ),
    CategoryEntity(
        name = "Học tập & phát triển bản thân",
        icon = "📚",
        description = "Đọc sách, học kỹ năng mới..."
    ),
    CategoryEntity(
        name = "Kỷ luật & sinh hoạt cá nhân",
        icon = "⏰",
        description = "Dậy sớm, ngủ đúng giờ..."
    ),
    CategoryEntity(
        name = "Mối quan hệ & xã hội",
        icon = "💬",
        description = "Kết bạn mới, hỏi thăm họ hàng..."
    )
)
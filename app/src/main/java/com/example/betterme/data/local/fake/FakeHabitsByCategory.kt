package com.example.betterme.data.local.fake

data class FakeHabitGroup(
    val categoryId: Int,
    val categoryName: String,
    val categoryIcon: String,
    val habits: List<String>
)

fun fakeHabitGroups(): List<FakeHabitGroup> = listOf(
    FakeHabitGroup(
        categoryId = 1,
        categoryName = "Vận động & thể chất",
        categoryIcon = "🏃",
        habits = listOf(
            "Đi bộ 10.000 bước mỗi ngày",
            "Tập Gym 30 phút",
            "Dãn cơ 15 phút mỗi sáng",
            "Đạp xe 20 phút",
            "Chạy bộ buổi sáng 3km"
        )
    ),
    FakeHabitGroup(
        categoryId = 2,
        categoryName = "Dinh dưỡng & ăn uống lành mạnh",
        categoryIcon = "🥗",
        habits = listOf(
            "Uống đủ 2 lít nước mỗi ngày",
            "Ăn sáng đủ chất dinh dưỡng",
            "Ăn rau xanh mỗi bữa",
            "Hạn chế đồ ngọt & nước có ga",
            "Meal prep vào cuối tuần"
        )
    ),
    FakeHabitGroup(
        categoryId = 3,
        categoryName = "Tinh thần & sức khỏe tâm lý",
        categoryIcon = "🧠",
        habits = listOf(
            "Thiền 10 phút mỗi sáng",
            "Viết nhật ký trước khi ngủ",
            "Tập hít thở sâu 5 phút",
            "Nghe nhạc thư giãn 15 phút",
            "Tắt điện thoại 30 phút trước ngủ"
        )
    ),
    FakeHabitGroup(
        categoryId = 4,
        categoryName = "Học tập & phát triển bản thân",
        categoryIcon = "📚",
        habits = listOf(
            "Đọc sách 20 phút mỗi ngày",
            "Học 10 từ vựng tiếng Anh",
            "Nghe podcast 15 phút",
            "Viết blog hoặc ghi chú kiến thức",
            "Xem 1 bài giảng online"
        )
    ),
    FakeHabitGroup(
        categoryId = 5,
        categoryName = "Kỷ luật & sinh hoạt cá nhân",
        categoryIcon = "⏰",
        habits = listOf(
            "Dậy sớm lúc 6h sáng",
            "Đi ngủ trước 23h",
            "Dọn dẹp phòng mỗi sáng",
            "Lập kế hoạch công việc buổi sáng",
            "Không dùng điện thoại sau 22h"
        )
    ),
    FakeHabitGroup(
        categoryId = 6,
        categoryName = "Mối quan hệ & xã hội",
        categoryIcon = "💬",
        habits = listOf(
            "Gọi điện cho gia đình mỗi ngày",
            "Kết bạn mới mỗi tuần",
            "Viết thư cảm ơn 1 người",
            "Tham gia 1 hoạt động nhóm",
            "Hỏi thăm bạn bè mỗi ngày"
        )
    )
)

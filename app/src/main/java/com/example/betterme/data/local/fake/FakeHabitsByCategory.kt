package com.example.betterme.data.local.fake

data class FakeHabitGroup(
    val categoryId: Int,
    val categoryName: String,
    val categoryIcon: String,
    val habits: List<String>
)

// ============================================================
// POOL DỮ LIỆU — mỗi category có 12 thói quen
// Khi "AI" chọn, sẽ dùng userId làm seed để shuffle & lấy 5
// → Mỗi người dùng thấy combo thói quen khác nhau
// ============================================================
private val habitPool: List<FakeHabitGroup> = listOf(
    FakeHabitGroup(
        categoryId = 1,
        categoryName = "Vận động & thể chất",
        categoryIcon = "🏃",
        habits = listOf(
            "Đi bộ 10.000 bước mỗi ngày",
            "Tập Gym 30 phút",
            "Dãn cơ 15 phút mỗi sáng",
            "Đạp xe 20 phút",
            "Chạy bộ buổi sáng 3km",
            "Tập yoga 20 phút trước ngủ",
            "Bơi lội 3 lần mỗi tuần",
            "Hít đất 20 cái mỗi ngày",
            "Tập plank 3 phút mỗi sáng",
            "Leo cầu thang thay thang máy",
            "Nhảy dây 100 cái mỗi ngày",
            "Đi bộ nhanh 30 phút buổi chiều"
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
            "Meal prep vào cuối tuần",
            "Uống nước ấm sau khi thức dậy",
            "Ăn chậm và nhai kỹ",
            "Không ăn khuya sau 21h",
            "Thêm trái cây vào bữa sáng",
            "Hạn chế ăn đồ chiên rán",
            "Ăn đủ 3 bữa đúng giờ",
            "Bổ sung vitamin D mỗi ngày"
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
            "Tắt điện thoại 30 phút trước ngủ",
            "Viết 3 điều biết ơn mỗi ngày",
            "Đặt mục tiêu nhỏ hàng ngày",
            "Giảm thời gian mạng xã hội còn 1h",
            "Đọc sách tâm lý 15 phút",
            "Thực hành affirmation buổi sáng",
            "Đi dạo trong thiên nhiên 20 phút",
            "Thực hành chánh niệm khi ăn"
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
            "Xem 1 bài giảng online",
            "Luyện nghe tiếng Anh 20 phút",
            "Tóm tắt 1 điểm học được mỗi ngày",
            "Hoàn thành 1 chương sách mỗi tuần",
            "Học kỹ năng mới 30 phút",
            "Ôn lại kiến thức cũ 10 phút",
            "Đăng ký 1 khóa học online",
            "Thực hành nói tiếng Anh 10 phút"
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
            "Không dùng điện thoại sau 22h",
            "Chuẩn bị quần áo từ tối hôm trước",
            "Làm việc theo Pomodoro 25 phút",
            "Review lại kế hoạch trước khi ngủ",
            "Không trì hoãn việc quan trọng",
            "Đặt báo thức không snooze",
            "Tắt thông báo khi tập trung làm việc",
            "Dọn dẹp bàn làm việc trước khi bắt đầu"
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
            "Hỏi thăm bạn bè mỗi ngày",
            "Dành 30 phút chất lượng cho gia đình",
            "Chủ động lắng nghe khi giao tiếp",
            "Gửi tin nhắn động viên cho bạn bè",
            "Tham gia câu lạc bộ hoặc hội nhóm",
            "Tổ chức buổi họp mặt bạn bè",
            "Học cách bày tỏ cảm xúc tích cực",
            "Tình nguyện 1 lần mỗi tháng"
        )
    )
)

// ============================================================
// HÀM PERSONALIZATION
// Dùng userId.hashCode() làm seed → mỗi user nhận combo khác
// nhau nhưng luôn nhất quán cho cùng 1 user
// ============================================================
fun getPersonalizedHabitGroups(userId: String): List<FakeHabitGroup> {
    return habitPool.map { group ->
        // Kết hợp userId + categoryId để seed khác nhau cho từng nhóm
        val seed = (userId + group.categoryId.toString()).hashCode().toLong()
        val shuffled = group.habits.shuffled(java.util.Random(seed))
        group.copy(habits = shuffled.take(5))
    }
}

// ============================================================
// HÀM CŨ (giữ lại để không break code nào đang dùng)
// ============================================================
fun fakeHabitGroups(): List<FakeHabitGroup> = habitPool.map { it.copy(habits = it.habits.take(5)) }

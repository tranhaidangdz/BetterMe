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
            "Chạy bộ buổi sáng 3km",
            "Plank 3 hiệp mỗi ngày",
            "Leo cầu thang thay thang máy",
            "Tập squat 50 cái mỗi ngày",
            "Nhảy dây 10 phút",
            "Khởi động 5 phút sau khi thức dậy"
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
            "Không ăn sau 20h",
            "Bổ sung trái cây 2 phần mỗi ngày",
            "Ăn chậm, nhai kỹ trong 20 phút",
            "Chuẩn bị bữa trưa healthy mang đi làm",
            "Theo dõi lượng calo mỗi ngày"
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
            "Viết 3 điều biết ơn mỗi tối",
            "Đi bộ chậm 15 phút để thư giãn",
            "Đọc sách nhẹ nhàng trước khi ngủ",
            "Thực hành self-talk tích cực 5 phút",
            "Không làm việc trong giờ nghỉ trưa"
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
            "Ôn tập theo phương pháp Pomodoro 25 phút",
            "Làm 5 câu hỏi luyện tập mỗi ngày",
            "Tóm tắt kiến thức trong 1 trang",
            "Học kỹ năng mới 30 phút",
            "Đặt mục tiêu học tập cho ngày hôm sau"
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
            "Kiểm tra to-do list vào đầu ngày",
            "Chuẩn bị quần áo/tài liệu từ tối hôm trước",
            "Hoàn thành việc khó nhất trước 10h",
            "Review ngày làm việc trước khi ngủ",
            "Giữ bàn làm việc gọn gàng 10 phút cuối ngày"
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
            "Gửi lời khen chân thành cho 1 người",
            "Nhắn tin reconnect với bạn cũ",
            "Ăn trưa cùng đồng nghiệp 1 lần/tuần",
            "Luyện lắng nghe chủ động 10 phút mỗi ngày",
            "Chủ động giúp đỡ 1 người xung quanh"
        )
    )
)

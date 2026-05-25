# UC03 — Xem danh sách thói quen

**Tác nhân:** Người dùng (chính)

**Mô tả ngắn:** Người dùng xem danh sách các thói quen đang theo dõi cùng trạng thái hôm nay, có thể lọc theo nhóm.

**Điều kiện trước:**
- Đã đăng nhập.
- Có ít nhất 0 thói quen (cho phép rỗng).

**Điều kiện sau:**
- Hiển thị danh sách thói quen đầy đủ.
- Trạng thái check-in hôm nay được tính toán.

## Luồng sự kiện chính
1. Người dùng chọn tab "Thói quen" trên BottomNav.
2. `HabitsViewModel` gọi `HabitRepository.getAll(userId)` (Flow Room).
3. `Ctrl_ThoiQuen` truy vấn song song `habit_logs` của ngày hôm nay.
4. Hệ thống tính trạng thái done/pending cho mỗi thói quen.
5. `GD_ThoiQuen` render `LazyColumn` các thẻ thói quen.
6. Người dùng cuộn / lọc theo nhóm category.

## Luồng sự kiện phụ
- 5.1. Danh sách rỗng → hiển thị empty state kèm CTA "Thêm thói quen đầu tiên".

## Luồng ngoại lệ
- Lỗi truy vấn Room → snackbar "Không tải được dữ liệu" + nút thử lại.

## Liên hệ tới mã nguồn
- File: `app/src/main/java/.../presentation/habits/HabitsScreen.kt`
- ViewModel: `HabitsViewModel`
- Repository: `HabitRepository, HabitLogRepository`

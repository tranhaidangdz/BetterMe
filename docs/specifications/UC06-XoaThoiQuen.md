# UC06 — Xóa thói quen

**Tác nhân:** Người dùng (chính), AlarmManager (phụ)

**Mô tả ngắn:** Người dùng xoá một thói quen; hệ thống xoá mềm và huỷ alarm tương ứng.

**Điều kiện trước:**
- Tồn tại thói quen muốn xoá.

**Điều kiện sau:**
- `habits` được đánh dấu `isArchived = true` (xoá mềm).
- `AlarmManager` huỷ lịch nhắc.

## Luồng sự kiện chính
1. Người dùng vuốt thẻ thói quen hoặc mở chi tiết và nhấn "Xoá".
2. Hệ thống hiển thị dialog xác nhận.
3. Người dùng xác nhận.
4. `Ctrl_ThoiQuen` softDelete `habits` (Room).
5. Huỷ `AlarmManager` reminders liên quan.
6. Đồng bộ Firestore.
7. Quay về danh sách, hiển thị snackbar "Đã xoá" + nút "Hoàn tác".

## Luồng sự kiện phụ
- 7.1. Nhấn "Hoàn tác" trong 5s → revert `isArchived = false` và đặt lại alarm.

## Luồng ngoại lệ
- Người dùng huỷ dialog → giữ nguyên.

## Liên hệ tới mã nguồn
- File: `app/src/main/java/.../presentation/habits/HabitDetailScreen.kt`
- ViewModel: `HabitDetailViewModel`
- Repository: `HabitRepository, ReminderRepository`

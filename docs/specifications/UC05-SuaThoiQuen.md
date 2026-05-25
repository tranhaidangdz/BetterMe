# UC05 — Chỉnh sửa thói quen

**Tác nhân:** Người dùng (chính), AlarmManager (phụ)

**Mô tả ngắn:** Người dùng chỉnh sửa các trường của một thói quen đã có.

**Điều kiện trước:**
- Tồn tại thói quen muốn sửa.

**Điều kiện sau:**
- `habits` được cập nhật.
- AlarmManager được re-schedule nếu thời gian nhắc thay đổi.

## Luồng sự kiện chính
1. Người dùng mở `GD_ChiTietThoiQuen` của thói quen cần sửa.
2. Người dùng nhấn "Chỉnh sửa".
3. Hệ thống mở form điền sẵn các giá trị hiện tại.
4. Người dùng sửa các trường cần thiết.
5. Người dùng nhấn "Lưu".
6. `Ctrl_ThoiQuen` validate và `update(habit)` vào Room.
7. Re-schedule AlarmManager nếu `reminderTime` thay đổi.
8. SyncCoordinator đồng bộ Firestore.

## Luồng sự kiện phụ
- Không có.

## Luồng ngoại lệ
- Trường không hợp lệ → báo lỗi inline.
- Mất mạng → vẫn ghi Room, pending sync.

## Liên hệ tới mã nguồn
- File: `app/src/main/java/.../presentation/habits/edit/EditHabitScreen.kt`
- ViewModel: `EditHabitViewModel`
- Repository: `HabitRepository`

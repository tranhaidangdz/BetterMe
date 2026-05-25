# UC04 — Tạo thói quen mới

**Tác nhân:** Người dùng (chính), AlarmManager (phụ)

**Mô tả ngắn:** Người dùng tạo mới một thói quen với tên, mô tả, tần suất, nhóm và (tùy chọn) thời điểm nhắc nhở.

**Điều kiện trước:**
- Đã đăng nhập.
- Đã hoàn thành Onboarding để có ít nhất một nhóm.

**Điều kiện sau:**
- Bản ghi `habits` được thêm vào Room.
- Nếu có nhắc, AlarmManager được đặt lịch.
- SyncWorker đẩy bản ghi lên Firestore.

## Luồng sự kiện chính
1. Người dùng nhấn FAB "Thêm thói quen" trên `GD_ThoiQuen`.
2. Hệ thống mở `GD_ThemThoiQuen` (AddHabitScreen).
3. Người dùng nhập tên, mô tả, mục tiêu/ngày, tần suất, nhóm.
4. Người dùng (tuỳ chọn) bật nhắc nhở và chọn thời gian.
5. Người dùng nhấn "Lưu".
6. `Ctrl_ThoiQuen` validate dữ liệu (tên không rỗng, target > 0).
7. Insert `habits` + `reminders` (nếu có) vào Room.
8. Đặt `AlarmManager.setRepeating(...)` cho từng thời điểm nhắc.
9. Đẩy bản ghi qua `SyncCoordinator` → Firestore.
10. Hệ thống đóng form và toast "Đã thêm thói quen".

## Luồng sự kiện phụ
- 7.1. Lưu khi mạng yếu → ghi Room, đánh dấu pending sync.

## Luồng ngoại lệ
- Tên rỗng / target ≤ 0 → hiển thị lỗi inline, không đóng form.
- AlarmManager không có quyền SCHEDULE_EXACT_ALARM (API 31+) → fallback inexact.

## Liên hệ tới mã nguồn
- File: `app/src/main/java/.../presentation/habits/add/AddHabitScreen.kt`
- ViewModel: `AddHabitViewModel`
- Repository: `HabitRepository, ReminderRepository`

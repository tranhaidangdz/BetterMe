# UC09 — Nhận thông báo nhắc nhở

**Tác nhân:** AlarmManager (chính kích hoạt), Người dùng (nhận)

**Mô tả ngắn:** Hệ thống đẩy thông báo cho người dùng đúng thời điểm đã đặt, mở chi tiết thói quen khi tap.

**Điều kiện trước:**
- Đã có `reminders` được lên lịch.

**Điều kiện sau:**
- Notification hiển thị (nếu chưa check-in hôm nay).

## Luồng sự kiện chính
1. Đến thời điểm cài đặt, AlarmManager kích hoạt `HabitReminderReceiver`.
2. `Ctrl_NotificationService` truy vấn `habit_logs` hôm nay.
3. Nếu chưa hoàn thành → đẩy notification "Đã đến giờ <tên thói quen>".
4. Người dùng tap vào thông báo.
5. Hệ thống mở `HabitDetailScreen` qua deep link.

## Luồng sự kiện phụ
- 3.1. Đã hoàn thành hôm nay → bỏ qua, không hiển thị notif.

## Luồng ngoại lệ
- Người dùng tắt notif → AlarmManager vẫn fire nhưng không show.
- Sau reboot, `HabitBootReceiver` phục hồi lịch.

## Liên hệ tới mã nguồn
- File: `app/src/main/java/.../background/reminder/HabitReminderReceiver.kt`
- File: `HabitBootReceiver.kt`
- ViewModel: `-`
- Repository: `ReminderRepository, HabitLogRepository`

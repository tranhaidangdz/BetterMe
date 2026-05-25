# UC07 — Xem chi tiết thói quen

**Tác nhân:** Người dùng (chính)

**Mô tả ngắn:** Người dùng xem chi tiết một thói quen: mô tả, streak, % hoàn thành, heatmap, lịch sử check-in.

**Điều kiện trước:**
- Tồn tại thói quen.

**Điều kiện sau:**
- Màn hình chi tiết hiển thị đầy đủ thông tin.

## Luồng sự kiện chính
1. Người dùng chạm vào một thói quen trong danh sách.
2. `HabitDetailViewModel` load `habits` + 30 log gần nhất.
3. `Ctrl_ChartService` xây dựng heatmap.
4. Hệ thống tính streak và tỉ lệ hoàn thành.
5. `GD_ChiTietThoiQuen` render biểu đồ + danh sách log.
6. Người dùng có thể cuộn xem ảnh check-in từng ngày.

## Luồng sự kiện phụ
- Không có.

## Luồng ngoại lệ
- Habit không tồn tại / bị xoá → chuyển về danh sách.

## Liên hệ tới mã nguồn
- File: `app/src/main/java/.../presentation/habits/HabitDetailScreen.kt`
- ViewModel: `HabitDetailViewModel`
- Repository: `HabitRepository, HabitLogRepository`

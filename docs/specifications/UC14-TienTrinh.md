# UC14 — Theo dõi tiến trình thói quen

**Tác nhân:** Người dùng (chính)

**Mô tả ngắn:** Người dùng theo dõi streak, % hoàn thành 30 ngày và các cột mốc huy hiệu cá nhân.

**Điều kiện trước:**
- Tồn tại thói quen.

**Điều kiện sau:**
- Hiển thị streak, % và huy hiệu (nếu đạt cột mốc).

## Luồng sự kiện chính
1. Người dùng mở chi tiết thói quen.
2. `Ctrl_ThoiQuen` tính streak từ `habit_logs`.
3. Tính % hoàn thành 30 ngày.
4. Kiểm tra cột mốc (3, 7, 30, 100 ngày).
5. Nếu đạt cột mốc mới → trao huy hiệu (`user_achievements`) + popup chúc mừng.
6. Render `HeatmapView` + `ProgressBar`.

## Luồng sự kiện phụ
- Không có.

## Luồng ngoại lệ
- Chưa đạt cột mốc → chỉ hiển thị tiến độ.

## Liên hệ tới mã nguồn
- File: `app/src/main/java/.../presentation/habits/HabitDetailScreen.kt`
- ViewModel: `HabitDetailViewModel`
- Repository: `HabitRepository, AchievementRepository`

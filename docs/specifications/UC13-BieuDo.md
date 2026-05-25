# UC13 — Xem biểu đồ thống kê theo tuần / tháng

**Tác nhân:** Người dùng (chính)

**Mô tả ngắn:** Người dùng xem biểu đồ cột/đường về hoàn thành thói quen theo khoảng tuần hoặc tháng.

**Điều kiện trước:**
- Có dữ liệu `habit_logs`.

**Điều kiện sau:**
- Biểu đồ render đúng theo khoảng đã chọn.

## Luồng sự kiện chính
1. Người dùng mở tab "Thống kê".
2. Người dùng chọn khoảng tuần hoặc tháng.
3. `Ctrl_ChartService` gom logs theo ngày + nhóm.
4. Tính các metric: % hoàn thành, streak trung bình.
5. Render `BarChart` + `LineChart`.
6. Người dùng vuốt sang nhóm khác để xem chi tiết.

## Luồng sự kiện phụ
- Không có.

## Luồng ngoại lệ
- Dữ liệu rỗng → empty state.

## Liên hệ tới mã nguồn
- File: `app/src/main/java/.../presentation/stats/StatsScreen.kt`
- ViewModel: `StatsViewModel`
- Repository: `HabitLogRepository`

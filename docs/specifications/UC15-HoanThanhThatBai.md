# UC15 — Xem danh sách thói quen hoàn thành / thất bại

**Tác nhân:** Người dùng (chính)

**Mô tả ngắn:** Người dùng xem riêng danh sách thói quen đã hoàn thành hoặc bị bỏ lỡ theo bộ lọc.

**Điều kiện trước:**
- Có dữ liệu.

**Điều kiện sau:**
- Danh sách hiển thị đúng bộ lọc.

## Luồng sự kiện chính
1. Người dùng mở tab "Tổng kết".
2. Người dùng chọn bộ lọc: "Hoàn thành" / "Thất bại".
3. `Ctrl_ThongKe` query `habit_logs` theo bộ lọc.
4. Phân loại theo nhóm category.
5. Render danh sách kèm icon trạng thái.
6. Người dùng nhấn vào 1 mục để mở chi tiết.

## Luồng sự kiện phụ
- Không có.

## Luồng ngoại lệ
- Rỗng → empty state phù hợp với bộ lọc.

## Liên hệ tới mã nguồn
- File: `app/src/main/java/.../presentation/stats/SummaryScreen.kt`
- ViewModel: `SummaryViewModel`
- Repository: `HabitLogRepository`

# UC11 — Nhận phân tích thói quen từ AI

**Tác nhân:** Người dùng (chính), Gemini API (phụ)

**Mô tả ngắn:** AI phân tích dữ liệu thói quen trong 7-30 ngày gần nhất và đưa ra nhận định tổng quan.

**Điều kiện trước:**
- Có ít nhất 7 bản ghi `habit_logs`.

**Điều kiện sau:**
- Báo cáo phân tích hiển thị + cache 24h.

## Luồng sự kiện chính
1. Người dùng mở mục "Phân tích" trong tab AI.
2. `Ctrl_AIService` gom `habit_logs` 7-30 ngày qua.
3. Hệ thống tính tổng quan: streak, % hoàn thành theo nhóm.
4. Gọi AI prompt với `category = ANALYSIS`.
5. Lưu kết quả vào `ai_cache` (TTL 24h).
6. Render kết quả + biểu đồ minh hoạ.

## Luồng sự kiện phụ
- 1.1. Đã có cache mới → hiển thị ngay không gọi AI.

## Luồng ngoại lệ
- Dữ liệu quá ít → hiển thị "Cần thêm 7 ngày dữ liệu".
- AI lỗi → fallback OpenRouter, sau cùng hiển thị thông báo lỗi tử tế.

## Liên hệ tới mã nguồn
- File: `app/src/main/java/.../presentation/ai/InsightsScreen.kt`
- ViewModel: `InsightsViewModel`
- Repository: `AiChatRepository, HabitLogRepository`

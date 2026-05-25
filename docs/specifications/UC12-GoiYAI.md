# UC12 — Nhận gợi ý điều chỉnh thói quen từ AI

**Tác nhân:** Người dùng (chính), OpenRouter API (phụ)

**Mô tả ngắn:** AI đề xuất các điều chỉnh thói quen (thêm/sửa/tần suất) dựa trên ngữ cảnh người dùng.

**Điều kiện trước:**
- Có ít nhất 1 thói quen.

**Điều kiện sau:**
- Danh sách gợi ý được hiển thị; nếu áp dụng sẽ tạo/sửa habit tương ứng.

## Luồng sự kiện chính
1. Người dùng mở mục "Đề xuất AI".
2. `Ctrl_AIService` lấy ngữ cảnh: habits + logs gần đây.
3. Gọi AI prompt với `category = ADVICE`.
4. AI trả về danh sách gợi ý có cấu trúc (JSON).
5. Render danh sách thẻ gợi ý.
6. Người dùng nhấn "Áp dụng" trên một gợi ý.
7. Hệ thống tạo/sửa habit theo nội dung gợi ý.

## Luồng sự kiện phụ
- 3.1. Quota AI hết → trả về gợi ý fallback offline (heuristic).

## Luồng ngoại lệ
- Người dùng huỷ áp dụng → giữ nguyên gợi ý hiển thị.

## Liên hệ tới mã nguồn
- File: `app/src/main/java/.../presentation/ai/SuggestionsScreen.kt`
- ViewModel: `SuggestionsViewModel`
- Repository: `AiChatRepository, HabitRepository`

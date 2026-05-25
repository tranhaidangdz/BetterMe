# UC10 — Trò chuyện AI

**Tác nhân:** Người dùng (chính), Gemini API (phụ), OpenRouter API (phụ)

**Mô tả ngắn:** Người dùng đặt câu hỏi với AI coach; hệ thống ưu tiên cache → Gemini → fallback OpenRouter 7-model.

**Điều kiện trước:**
- Đã đăng nhập.
- Có kết nối Internet (trừ khi hit cache).

**Điều kiện sau:**
- Lưu cặp tin nhắn vào `ai_chat`.
- Lưu cache vào `ai_cache` (TTL 12-24h).

## Luồng sự kiện chính
1. Người dùng mở `GD_ChatAI`.
2. Người dùng nhập câu hỏi và gửi.
3. `Ctrl_AIService` (AiChatRouter) hash prompt và tra cứu `ai_cache`.
4. Nếu miss → gọi Gemini REST (singleFlight dedupe).
5. Gemini trả về câu trả lời.
6. Lưu `ai_cache` + `ai_chat`.
7. Stream tin nhắn lên UI.

## Luồng sự kiện phụ
- 3.1. Cache hit → trả về ngay, bỏ qua bước 4-5.
- 4.1. Gemini lỗi/timeout → fallback chain OpenRouter (xoay key, thử tuần tự 7 model).

## Luồng ngoại lệ
- Tất cả model fail → hiển thị "AI tạm thời không khả dụng".
- Mất mạng → chỉ có thể trả lời nếu hit cache.

## Liên hệ tới mã nguồn
- File: `app/src/main/java/.../presentation/ai/ChatAIScreen.kt`
- File: `app/src/main/java/.../domain/ai/AiChatRouter.kt`
- File: `SingleFlight.kt`
- ViewModel: `ChatAIViewModel`
- Repository: `AiChatRepository, AiCacheRepository`

# UC17 — Theo dõi tiến độ thử thách & nhận huy hiệu

**Tác nhân:** Người dùng (chính)

**Mô tả ngắn:** Hệ thống đánh giá tiến độ strict-daily, trao huy hiệu khi hoàn thành 100% và cho phép chia sẻ thành tích.

**Điều kiện trước:**
- Đã tham gia thử thách.

**Điều kiện sau:**
- Tiến độ cập nhật; huy hiệu được trao nếu hoàn thành.
- Có thể share text + ảnh check-in qua `ACTION_SEND_MULTIPLE`.

## Luồng sự kiện chính
1. Người dùng mở chi tiết thử thách đang tham gia.
2. `EvaluateChallengeStatusUseCase` truy vấn `challenge_logs` + `habit_logs`.
3. Tính số ngày đạt strict-daily liên tục.
4. Cập nhật `user_challenges.progress`.
5. Nếu đạt 100% → insert `user_achievements`, hiển thị popup chúc mừng.
6. Người dùng có thể nhấn "Chia sẻ".
7. `BuildChallengeProgressShareTextUseCase` build text + `ChallengeShareImagePrep` chuẩn bị URI ảnh.
8. Hệ thống mở `Intent.ACTION_SEND_MULTIPLE` để chia sẻ.

## Luồng sự kiện phụ
- 5.1. Strict-daily bị đứt (bỏ 1 ngày) → reset streak về 0 nhưng không xoá bản ghi tham gia.

## Luồng ngoại lệ
- Lỗi build share → fallback chỉ chia sẻ text.

## Liên hệ tới mã nguồn
- File: `app/src/main/java/.../domain/challenge/EvaluateChallengeStatusUseCase.kt`
- File: `BuildChallengeProgressShareTextUseCase.kt`
- File: `ChallengeShareImagePrep.kt`
- ViewModel: `ChallengeDetailViewModel`
- Repository: `ChallengeRepository, UserChallengeRepository, AchievementRepository`

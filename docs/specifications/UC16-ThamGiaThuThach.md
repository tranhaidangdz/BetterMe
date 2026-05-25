# UC16 — Tham gia thử thách cá nhân

**Tác nhân:** Người dùng (chính), WorkManager (phụ)

**Mô tả ngắn:** Người dùng đăng ký tham gia một thử thách strict-daily.

**Điều kiện trước:**
- Đã đăng nhập.

**Điều kiện sau:**
- Bản ghi `user_challenges` được tạo.
- `ChallengeReminderWorker` được enqueue.

## Luồng sự kiện chính
1. Người dùng mở tab "Thử thách".
2. `Ctrl_ThuThach` load danh sách `challenges` đang mở.
3. Người dùng chọn 1 thử thách.
4. Hệ thống hiển thị mô tả + luật strict-daily.
5. Người dùng nhấn "Tham gia".
6. Insert `user_challenges(userId, challengeId, startDate=today, progress=0)`.
7. Enqueue `ChallengeReminderWorker` định kỳ.
8. Hiển thị tiến độ ban đầu.

## Luồng sự kiện phụ
- Không có.

## Luồng ngoại lệ
- Đã tham gia rồi → nút chuyển thành "Đang tham gia", không tạo bản ghi mới.

## Liên hệ tới mã nguồn
- File: `app/src/main/java/.../presentation/challenge/ChallengeScreen.kt`
- ViewModel: `ChallengeViewModel`
- Repository: `ChallengeRepository, UserChallengeRepository`

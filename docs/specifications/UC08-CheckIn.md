# UC08 — Chụp ảnh check-in mỗi ngày

**Tác nhân:** Người dùng (chính), Cloudinary (phụ)

**Mô tả ngắn:** Người dùng đánh dấu hoàn thành thói quen kèm ảnh minh chứng được upload lên Cloudinary.

**Điều kiện trước:**
- Tồn tại thói quen.
- Có quyền CAMERA.

**Điều kiện sau:**
- Bản ghi `habit_logs` với `done = true` + `photoUrl`.
- `EvaluateChallengeStatusUseCase` cập nhật tiến độ thử thách liên quan.

## Luồng sự kiện chính
1. Người dùng nhấn nút "Check-in" trên chi tiết hoặc thẻ thói quen.
2. Hệ thống yêu cầu quyền CAMERA nếu chưa cấp.
3. Người dùng chụp ảnh hoặc chọn từ thư viện.
4. `Ctrl_ThoiQuen` upload ảnh tới Cloudinary qua `ImageUpload`.
5. Cloudinary trả về `photoUrl`.
6. Insert `habit_logs(habitId, date=today, done=true, photoUrl)`.
7. `Ctrl_ThuThach` gọi `EvaluateChallengeStatusUseCase` để cập nhật strict-daily challenge.
8. Hệ thống hiển thị popup chúc mừng.

## Luồng sự kiện phụ
- 2.1. Từ chối quyền → cho phép check-in không kèm ảnh.

## Luồng ngoại lệ
- Upload Cloudinary thất bại → vẫn ghi log với photoUrl null, đẩy retry sau.
- Đã check-in hôm nay → cho phép cập nhật ảnh.

## Liên hệ tới mã nguồn
- File: `app/src/main/java/.../presentation/habits/CheckInBottomSheet.kt`
- File: `app/src/main/java/.../data/upload/ImageUpload.kt`
- ViewModel: `HabitDetailViewModel`
- Repository: `HabitLogRepository, ChallengeRepository`

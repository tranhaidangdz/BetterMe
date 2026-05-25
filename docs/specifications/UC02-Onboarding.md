# UC02 — Xem Onboarding & chọn nhóm thói quen quan tâm

**Tác nhân:** Người dùng (chính)

**Mô tả ngắn:** Sau lần đăng nhập đầu tiên, người dùng được dẫn qua các slide giới thiệu và chọn các nhóm thói quen yêu thích để cá nhân hoá trải nghiệm.

**Điều kiện trước:**
- Đã đăng nhập thành công (UC01).
- Cờ `onboardingDone = false` trong bảng `users`.

**Điều kiện sau:**
- Bảng `user_categories` lưu các nhóm người dùng chọn.
- Cờ `onboardingDone = true` được cập nhật.
- Điều hướng tới `HomeScreen`.

## Luồng sự kiện chính
1. Người dùng được mở `GD_Onboarding` ngay sau đăng nhập.
2. Hệ thống hiển thị slide 1/3 (giới thiệu mục tiêu BetterMe).
3. Người dùng vuốt qua slide 2/3 và 3/3.
4. Người dùng nhấn "Bắt đầu".
5. `Ctrl_Onboarding` truy vấn 6 category mặc định từ Room.
6. Hệ thống hiển thị lưới các nhóm thói quen.
7. Người dùng tích chọn các nhóm quan tâm.
8. Người dùng nhấn "Tiếp tục".
9. `Ctrl_Onboarding` insert `user_categories` và set `onboardingDone = true`.
10. Hệ thống điều hướng tới `HomeScreen`.

## Luồng sự kiện phụ
- 7.1. Người dùng có thể bỏ trắng và nhấn "Bỏ qua"; hệ thống vẫn đặt `onboardingDone = true`.

## Luồng ngoại lệ
- Lỗi ghi Room → giữ ở Onboarding, hiển thị thông báo lỗi, cho thử lại.
- Người dùng thoát app giữa chừng → giữ trạng thái Onboarding, mở lại ở slide kế tiếp.

## Liên hệ tới mã nguồn
- File: `app/src/main/java/.../presentation/onboarding/OnboardingScreen.kt`
- ViewModel: `OnboardingViewModel`
- Repository: `CategoryRepository`, `UserCategoryRepository`, `UserRepository`

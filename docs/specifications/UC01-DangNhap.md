# UC01 — Đăng nhập / Xác thực người dùng

**Tác nhân:** Người dùng (chính), Firebase Auth (phụ), Firestore (phụ)

**Mô tả ngắn:** Người dùng đăng nhập vào ứng dụng BetterMe bằng tài khoản Google thông qua Credential Manager. Sau khi xác thực, hệ thống tạo/đồng bộ hồ sơ người dùng và mở màn hình chính.

**Điều kiện trước:**
- Thiết bị có kết nối Internet.
- Đã cài Google Play Services và có ít nhất một tài khoản Google.
- Người dùng chưa đăng nhập hoặc đã đăng xuất.

**Điều kiện sau:**
- Có phiên đăng nhập Firebase Auth hợp lệ.
- Bản ghi `users` tồn tại trong Room và Firestore (upsert).
- Điều hướng tới `HomeScreen` (hoặc Onboarding nếu là lần đầu).

## Luồng sự kiện chính
1. Người dùng mở ứng dụng BetterMe.
2. Hệ thống hiển thị `GD_DangNhap` với nút "Đăng nhập với Google".
3. Người dùng nhấn nút đăng nhập.
4. `Ctrl_Dangnhap` gọi Credential Manager để yêu cầu `idToken`.
5. Người dùng chọn tài khoản Google và xác nhận.
6. Hệ thống gọi `FirebaseAuth.signInWithCredential(idToken)`.
7. Firebase Auth trả về `AuthResult` chứa `uid`.
8. `Ctrl_Dangnhap` upsert bản ghi `users` (Room + Firestore qua SyncCoordinator).
9. Hệ thống điều hướng tới `HomeScreen`.

## Luồng sự kiện phụ
- 5a. Nếu là tài khoản mới đăng nhập lần đầu → sau bước 8 điều hướng sang luồng Onboarding (UC02) thay vì Home.
- 8a. Nếu Firestore chưa kết nối → ghi cục bộ Room, đánh dấu pending sync để SyncWorker đẩy sau.

## Luồng ngoại lệ
- Mất mạng → hiển thị toast "Không có kết nối, vui lòng thử lại"; giữ ở `GD_DangNhap`.
- Người dùng hủy chọn tài khoản → đóng dialog, trở lại nút đăng nhập.
- `idToken` không hợp lệ → Firebase trả lỗi, `Ctrl_Dangnhap` hiển thị "Đăng nhập thất bại, vui lòng thử lại".

## Liên hệ tới mã nguồn
- File: `app/src/main/java/.../presentation/auth/SignInScreen.kt`
- ViewModel: `SignInViewModel`
- Repository: `UserRepository`, `AuthRepository`
- Sync: `SyncCoordinator` + `UserSynchronizer`

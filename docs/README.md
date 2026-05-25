# Tài liệu UML — Dự án BetterMe

Thư mục này chứa toàn bộ tài liệu mô hình hoá theo chuẩn UML cho ứng dụng Android **BetterMe** (Kotlin + Jetpack Compose + Clean Architecture + Firebase + Room v14).

## Cấu trúc thư mục

```
docs/
├── README.md                    # Tệp này — mục lục tổng
├── diagrams/
│   └── BetterMe_UML.drawio      # Mxfile gốc, 40 trang
├── exports/
│   └── svg/                     # SVG xuất từ render_diagrams.mjs (40 tệp)
├── specifications/              # 17 đặc tả use case bằng tiếng Việt
│   ├── UC01-DangNhap.md
│   ├── UC02-Onboarding.md
│   └── …
└── tools/
    └── render_diagrams.mjs      # Node v24, không phụ thuộc — sinh 40 SVG
```

## Mục lục 40 hình

### Chương 2 — Sơ đồ Use Case (3 hình)
| Hình | Tên | Mô tả |
| :--: | :-- | :-- |
| 2.1 | UseCase tổng quát | Khung nhìn cao nhất — Người dùng + 6 hệ thống ngoài + 10 nhóm use case chính |
| 2.2 | UseCase người dùng (chi tiết) | Bao gồm các `«extend»` cho tạo/sửa/xoá/chi tiết thói quen, check-in, AI, thử thách |
| 2.3 | UseCase hệ thống / AI | Use case nội bộ + các actor `«external»`: Firebase Auth/Firestore/OpenRouter/Gemini/WorkManager/AlarmManager |

### Chương 3 — Sơ đồ Hoạt động (BĐHĐ) và Trình tự (BĐTT) (34 hình)
17 cặp BĐHĐ + BĐTT cho 17 use case:

| Hình BĐHĐ | Hình BĐTT | Use case |
| :--: | :--: | :-- |
| 3.1 | 3.1 | Đăng nhập / Xác thực người dùng |
| 3.2 | 3.2 | Xem Onboarding & chọn nhóm thói quen |
| 3.3 | 3.3 | Xem danh sách thói quen |
| 3.4 | 3.4 | Tạo thói quen mới |
| 3.5 | 3.5 | Chỉnh sửa thói quen |
| 3.6 | 3.6 | Xóa thói quen |
| 3.7 | 3.7 | Xem chi tiết thói quen |
| 3.8 | 3.8 | Chụp ảnh check-in mỗi ngày |
| 3.9 | 3.9 | Nhận thông báo nhắc nhở |
| 3.10 | 3.10 | Trò chuyện AI |
| 3.11 | 3.11 | Nhận phân tích thói quen từ AI |
| 3.12 | 3.12 | Nhận gợi ý điều chỉnh thói quen từ AI |
| 3.13 | 3.13 | Xem biểu đồ thống kê tuần / tháng |
| 3.14 | 3.14 | Theo dõi tiến trình thói quen |
| 3.15 | 3.15 | Xem danh sách hoàn thành / thất bại |
| 3.16 | 3.16 | Tham gia thử thách cá nhân |
| 3.17 | 3.17 | Theo dõi tiến độ thử thách & nhận huy hiệu |

### Chương 4 — Kiến trúc (3 hình)
| Hình | Tên | Mô tả |
| :--: | :-- | :-- |
| 4.1 | Sơ đồ Lớp | 12 thực thể domain + 5 repositories + 5 use case chủ chốt |
| 4.2 | Sơ đồ Thành phần | 4 tầng (Presentation / Domain / Data / Background) + 5 dịch vụ ngoài `«external»` |
| 4.3 | Sơ đồ Thực thể-Liên kết (ERD) | 16 bảng Room v14 với khoá chính + khoá ngoại |

## Quy ước trình bày

- **Phông chữ:** Arial — đồng nhất với tài liệu học thuật Việt Nam.
- **Màu sắc:** Chỉ dùng đen `#000000` (đường nét) và xám `#666666` (đường nét đứt cho `«include»`/`«extend»`). Tô nhẹ `#F0F0F0` cho header bảng, header lifeline, khoá chính.
- **Use case:** Người dùng dạng stick figure (`umlActor`); use case là ellipse; mối quan hệ `«include»`/`«extend»` dùng mũi tên đứt nét.
- **Sequence (BĐTT):** Lifeline có biểu tượng UML chuẩn — actor (Người dùng), boundary (`GD_*`), control (`Ctrl_*`), entity (`E_*`). Mũi tên đặc cho gọi đồng bộ; mũi tên đứt nét cho giá trị trả về. Khung `alt` dùng `umlFrame` cho nhánh thay thế.
- **Activity (BĐHĐ):** Tiêu đề "Người dùng | Hệ thống" trên cùng; node bắt đầu (chấm đen đặc) → action box bo tròn → quyết định (hình thoi) → node kết thúc.
- **Class / ERD:** Bảng (`shape=table`), dòng PK tô `#F0F0F0`, FK ghi tiền tố `FK:`.
- **Component:** Khối `shape=component` viền liền cho component nội bộ, đứt nét cho `«external»`.

## Cách dựng PNG

### 1. Sinh SVG từ Node
```powershell
node tools\render_diagrams.mjs
```
Lệnh ghi 40 tệp `*.svg` vào `docs/exports/svg/`. Mỗi SVG có thuộc tính `width` và `height` dạng số nguyên ở phần tử gốc, sẵn sàng cho bước rasterize.

### 2. Rasterize SVG → PNG (PowerShell helper)
Script PowerShell phụ trợ (do người dùng cung cấp) đọc `width`/`height` từ root SVG và xuất PNG cùng độ phân giải sang `docs/exports/png/`. Ví dụ chạy:
```powershell
.\tools\rasterize_svg_to_png.ps1   # do người dùng vận hành
```

### 3. Mở file mxfile gốc
Mở `diagrams/BetterMe_UML.drawio` bằng [draw.io desktop](https://www.diagrams.net/) hoặc <https://app.diagrams.net>. Có thể xuất PNG/PDF trực tiếp từ ứng dụng.

## Đối chiếu mã nguồn

Mỗi đặc tả `specifications/UCxx-*.md` đều có mục **"Liên hệ tới mã nguồn"** trỏ tới các file `*.kt`, `ViewModel`, `Repository`, và `UseCase` tương ứng trong `app/src/main/java/`.

## Sự thật nền tảng kỹ thuật BetterMe

- **Ngôn ngữ / UI:** Kotlin + Jetpack Compose + MVI/MVVM + Clean Architecture
- **DI:** Koin
- **Cơ sở dữ liệu:** Room v14 (16 bảng) với chiến lược nâng phiên bản phá huỷ
- **Firebase:** Auth (Google Sign-In qua Credential Manager), Firestore (bật persistence)
- **WorkManager:** `SyncWorker` (30 phút định kỳ + one-shot), `ChallengeReminderWorker`, `MidnightCleanupWorker`
- **AlarmManager:** `HabitReminderReceiver`, `HabitBootReceiver`
- **AI:** Gemini REST native + OpenRouter 7-model fallback chain; `AiChatRouter` xoay key; `SingleFlight` dedupe; `AiCacheRepository` TTL 12-24h
- **Sync:** `SyncCoordinator` + `ConnectivityObserver` + `SyncStatusRepository` + 7 `EntitySynchronizer` + chiến lược `LastWriteWins`
- **Thử thách:** Đánh giá strict-daily qua `EvaluateChallengeStatusUseCase`
- **Chia sẻ:** `ChallengeShareImagePrep` + `Intent.ACTION_SEND_MULTIPLE` + `BuildChallengeProgressShareTextUseCase`
- **Upload ảnh:** Cloudinary

## Riêng cho app Flutter (`/Volumes/Code/event_app`)

- **Cấu trúc:** `lib/app/modules/<tính-năng>/` (màn hình + controller cùng file hoặc cạnh nhau), `lib/app/data/models/`, `lib/app/data/services/` (mỗi nhóm API một lớp `*_api.dart`, đăng ký bằng `Get.put(..., permanent: true)` trong `lib/main.dart`), `lib/core/` (mạng, theme, env, tiện ích dùng chung), `lib/app/widgets/`.
- **Thêm màn mới:** khai báo route trong `app_routes.dart` và `app_pages.dart`; không điều hướng bằng chuỗi rời rạc.
- **GetX:** controller kế thừa `BaseController` (có `isLoading`, `handleError`, `showError`, `showSuccess`). **`Obx` phải đọc `.value` ngay trong closure của chính nó**, đọc bên trong `build` của widget con sẽ không cập nhật. Màn hình có thể mở chồng lên chính nó (chi tiết show → show liên quan) thì tạo controller với `tag` riêng và `Get.delete` khi `dispose`.
- **Model:** `fromJson` đọc đúng khoá snake_case; trường tuỳ chọn là nullable, danh sách mặc định rỗng. Bóc envelope bằng `ApiResponse`.
- **Màu:** dùng `AppColors` (`primary`, `gold`, `surface`...), không `Color(0x...)` lẻ ngoại trừ màu sao đánh giá.
- **Ảnh mạng:** dùng `NetImage` (có khung chờ, ảnh lỗi, `cacheWidth`). Không tải ảnh gốc kích thước lớn vào danh sách.
- **Môi trường:** không hard-code URL; dùng `EnvMode` và `env/*.json`. Chạy thử: `./scripts/flutter_env.sh run -d <thiết-bị>`.
- **Kiểm tra trước khi báo xong:** `flutter analyze` không có lỗi hay cảnh báo mới (7 thông báo `use_null_aware_elements` có sẵn được bỏ qua) và chạy thử trên iOS simulator; tính năng liên quan hệ thống phải thử cả **Android thật**.
- **Format:** `dart format` **chỉ** cho file mình vừa sửa và chỉ khi file đó đã được format từ trước. Không bao giờ `dart format lib` (từng làm nhiễu 74 file).
- **Thư viện và dung lượng:** app phải nhẹ (đích ~24MB AAB). Muốn thêm thư viện: hỏi anh Nghĩa, ghi dung lượng tăng vào PLAN.md. Quyền hệ thống mới thì cập nhật `Info.plist`/`AndroidManifest` **và** nhắc khai báo Data safety.
- **Bản phát hành:** `./scripts/build_release.sh appbundle|apk` (arm64, làm rối mã, giữ `build/symbols`). Mỗi lần upload Google Play phải **tăng `versionCode`** (số sau dấu `+` trong `pubspec.yaml`). Kiểm tra đúng nhánh, đo dung lượng, ghi vào PLAN.md.
- **Bản v1 giới hạn lân sư rồng** bằng `kOnlyCategory` (`lib/core/config/release_scope.dart`); đừng xoá ở nhánh `v1-lan-su-rong`.
- **Biểu tượng, màn chào, tên app:** nguồn ở `assets/branding_src/`; sinh lại bằng `flutter_launcher_icons` và `flutter_native_splash`.

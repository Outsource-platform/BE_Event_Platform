<!-- BEGIN:stagio-rules -->

# Stagio — quy tắc làm việc (tự sinh từ `docs/rules/`, đừng sửa tay)

> **Kế hoạch và tiến độ sống:** `/Users/macos/Desktop/SaaS/event-platform/docs/PLAN.md`
> Đọc file đó trước khi làm, cập nhật nó trước khi kết thúc lượt. Quy tắc gốc: `/Users/macos/Desktop/SaaS/event-platform/docs/rules/`.

## 1. Việc đầu tiên và việc cuối cùng của mỗi lượt làm

1. **Đọc kế hoạch trước khi viết code:** `/Users/macos/Desktop/SaaS/event-platform/docs/PLAN.md`. Xem mục 0 (việc tiếp theo, việc đang làm dở, câu hỏi chờ anh Nghĩa) và mục liên quan tới việc được giao. Nếu mục 0.2 có việc dở thì làm nốt trước.
2. **Cập nhật kế hoạch trong cùng lượt làm:** tick ô đã xong, sửa trạng thái, thêm **một dòng** vào "Nhật ký tiến độ", sửa dòng "Cập nhật lần cuối". Cách làm chi tiết ở mục 9 của PLAN.md.
3. **Phải dừng giữa chừng thì ghi vào mục 0.2 của PLAN.md** (việc gì, làm đến đâu, file nào dở, bước kế tiếp) để người sau (AI hay người) làm tiếp được. Không để code dở mà không ghi.
4. Các repo của dự án: app Flutter `/Volumes/Code/event_app`, backend `/Users/macos/Desktop/SaaS/event-platform`, web `/Users/macos/Desktop/SaaS/admin-web`. Tính năng chạm nhiều repo thì sửa đủ cả backend, app, web (hoặc ghi rõ phần chưa làm vào PLAN.md) trong cùng đợt.

## 2. Cách làm việc chung

- **Ngôn ngữ:** trả lời anh Nghĩa bằng **tiếng Việt, ngắn gọn, nói thẳng**. Chuỗi hiển thị cho người dùng, bình luận trong code, thông điệp commit đều viết **tiếng Việt có dấu**; tên biến, hàm, file bằng tiếng Anh.
- **Bình luận trong code** giải thích *vì sao*, ngắn; không nhắc lại điều code đã nói. Không để code chết, `print` thừa, TODO không rõ người làm.
- **Làm đúng việc được giao.** Không refactor lan, không đổi định dạng hàng loạt, không thêm thư viện khi chưa cần. Việc ngoài phạm vi thấy được thì ghi vào mục 6 của PLAN.md thay vì tự sửa.
- **Không lật các quyết định đã chốt** ở mục 7 của PLAN.md khi chưa hỏi anh Nghĩa.
- **Báo trung thực:** chỉ nói "xong" khi đã build/analyze và chạy thử được. Phần chưa thử thì nói rõ "chưa thử" và ghi vào PLAN.md (ký hiệu 🟡). Lỗi test thất bại thì báo kèm kết quả, không giấu.
- Việc khó đảo ngược hoặc đụng bên ngoài (push, xoá, deploy lên production, gửi tin, chi tiền) phải **hỏi trước**.

## 3. Git

- Nhánh theo mục 3 của PLAN.md. App: `release/v1-lan-su-rong` là bản **đang thử nghiệm trên Google Play, đóng băng**, chỉ sửa lỗi bắt buộc; mọi tính năng mới làm ở `release/v1.1-bang-tin`. Trước khi sửa hoặc build, chạy `git branch --show-current`.
- Commit nhỏ, mỗi commit một ý, thông điệp tiếng Việt nêu việc và lý do. Chỉ `git add` đúng file của mình; **không `git add -A`** khi cây làm việc còn việc chưa commit của người khác.
- **Không push, không force-push, không viết lại lịch sử** nếu anh Nghĩa chưa bảo.
- AI (Claude) thêm dòng `Co-Authored-By: Claude <noreply@anthropic.com>` ở cuối thông điệp commit. Cursor và người thì không cần.
- File sinh tự động (`generated_plugin_registrant`, `pubspec.lock`, `package-lock.json`) chỉ commit khi thêm/bỏ thư viện.

## 4. Bí mật và dữ liệu nhạy cảm

- **Không bao giờ commit hay dán vào chat/log:** file `.env`, `android/key.properties`, `*.jks` (khoá ký), khoá SSH `.pem`, `DEMO-ACCOUNTS.txt` (mật khẩu demo), token, mật khẩu, khoá API thật. Giá trị mẫu đặt trong `.env.example`.
- Cấu hình khác nhau theo môi trường đi qua biến môi trường hoặc `env/*.json`, không hard-code URL hay khoá trong code.
- API công khai **không trả** địa chỉ, số điện thoại, tên đầy đủ khách, số tiền, mã show. Kiểm tra lại mỗi khi thêm trường vào DTO công khai.

## 5. Server và triển khai (rất quan trọng)

- Server dev `rocky@124.197.20.141` (khoá `/Users/macos/Desktop/Rencity/SSH Key.pem`) **dùng chung với project của người khác** (recity_web, back_end_gg, nginx-app-1, redis/rabbitmq riêng). **Chỉ được thao tác trong `~/BE_Event_Platform`.**
- Luôn `cd ~/BE_Event_Platform` rồi mới chạy `docker compose`, và chỉ định đích danh service: `docker compose up -d --build <tên-service>`.
- **Cấm:** `docker compose down -v`, `docker system prune`, `docker stop $(docker ps -q)`, mọi lệnh docker toàn cục, sửa nginx hay project khác.
- Quy trình deploy một service: sửa code → `mvn -q -DskipTests package` ở gốc và **kiểm tra mã thoát** → commit + push `master` (khi anh Nghĩa cho phép) → trên server `git pull` → `mvn -q -pl services/<tên> -am -DskipTests package` → `docker compose up -d --build <tên>` → đợi log "Started" rồi kiểm tra bằng `curl`.
- Server chỉ còn ~750MB RAM trống: không chạy tác vụ nặng liên tục. Kiểm tra `free -m` trước khi làm việc tốn bộ nhớ.
- Một file `.env` duy nhất ở gốc `~/BE_Event_Platform`. Compose không dùng `env_file`; biến phải được khai trong `docker-compose.yml`.
- Dữ liệu demo chỉ dành cho server dev, bật bằng `DEMO_SEED_ENABLED`. **Không bật trên production** trừ khi có chủ đích.

## 6. Hợp đồng API giữa backend, app và web

- JSON **snake_case** hai chiều. Mọi response bọc `{ "success", "code", "message", "data" }` (trừ 204, ảnh/video/file).
- Lỗi nghiệp vụ dùng mã `code` ổn định (`ApiException`), thông điệp `message` tiếng Việt; client rẽ nhánh theo `code`, không theo chuỗi.
- **Chỉ thêm, không đổi tên hay xoá** trường/endpoint đang dùng: hai bản app (test và cập nhật) chạy song song trên cùng backend.
- Thay đổi hợp đồng (endpoint, trường) phải cập nhật tại chỗ: backend, mô hình và API trong app, web, và ghi vào PLAN.md.
- Phân trang trả `{ items, page, size, total_elements, total_pages }`.

## 7. Giao diện và trải nghiệm

- Toàn bộ chữ tiếng Việt chuẩn có dấu. Ngày `dd/MM/yyyy`, tiền `1.000.000 đ`.
- **Biểu mẫu dùng bottom sheet** trượt từ đáy, không dùng hộp thoại (AlertDialog) giữa màn.
- Màu lấy từ theme của app/web (đoàn có màu thương hiệu riêng), không hard-code màu.
- Màn hình quan trọng cho việc chốt show (chi tiết show) phải để nút hành động **cố định, thấy ngay**, không bắt người dùng cuộn mới thấy.
- Trạng thái rỗng, đang tải, lỗi đều phải có giao diện rõ ràng; lỗi mạng không được làm app văng.

## Riêng cho backend (`/Users/macos/Desktop/SaaS/event-platform`)

- **Công nghệ:** Java 17, Spring Boot 4 (Jackson 3: lớp xử lý nằm ở `tools.jackson...`, annotation vẫn là `com.fasterxml.jackson.annotation`), Maven nhiều module (`libs/shared-common`, `services/*`), MySQL, Redis, RabbitMQ. Mỗi service có DB riêng, **không join qua service**; chỉ truyền id.
- **Phân quyền:** dùng `@PreAuthorize("hasRole('ADMIN'|'CUSTOMER'|'SUPER_ADMIN')")`. **`tenantId` luôn lấy từ `JwtPrincipal`**, không bao giờ tin `tenantId` trong body hay URL; mọi thao tác trên bản ghi của đơn vị phải kiểm tra bản ghi thuộc `tenantId` của người gọi.
- **Gateway:** endpoint mới phải có đường trong `RouteTable`; endpoint công khai thêm vào `PUBLIC_ROUTES` của `GatewayProxyFilter` (và cấu hình bảo mật của service). Sửa gateway thì deploy cả gateway. Đường `/api/internal/**` không đi qua gateway, dùng `X-Internal-Token`, không bọc envelope.
- **Phản hồi:** trả DTO/`record`; `ResponseWrappingAdvice` tự bọc envelope và bỏ qua `byte[]`, `Resource`, `ResourceRegion`. Không tự bọc `ApiResponse` trong controller.
- **Lỗi nghiệp vụ:** `new ApiException(HttpStatus, "MA_LOI_ON_DINH", "Thông điệp tiếng Việt")`. Không ném `RuntimeException` trần cho lỗi người dùng có thể gặp.
- **CSDL:** `ddl-auto=update` chỉ thêm bảng và cột. Cột mới phải **nullable hoặc có giá trị mặc định** để dữ liệu cũ không hỏng; đổi tên hay xoá cột cần kế hoạch migrate riêng và hỏi anh Nghĩa. Thêm `@Index` cho cột tra cứu thường xuyên.
- **Gọi giữa service:** qua `InternalRestClients` (có timeout), có cache ngắn bằng `TtlCache` cho dữ liệu công khai; endpoint công khai trả kèm `Cache-Control`.
- **Tránh N+1:** tải theo lô (`findAllById`, `...In(...)`) khi dựng danh sách.
- **Tải tệp:** kiểm tra đuôi tệp **và** content-type, giới hạn dung lượng, không tin tên tệp từ client. Ảnh → WebP, video → H.264 bằng ffmpeg (container catalog có ffmpeg, `mem_limit` 640MB); không có ffmpeg thì giữ bản gốc, không làm hỏng việc tải lên.
- **Bộ nhớ:** mỗi service có `mem_limit` và `JAVA_OPTS` riêng trong Dockerfile/compose; tác vụ nặng chạy nền, một cái một lúc.
- **Dữ liệu demo:** seeder đặt trong gói `config` hoặc `service`, gắn `@ConditionalOnProperty(name = "demo.seed.enabled", havingValue = "true")`, chạy lại không tạo trùng.
- **Xây dựng và kiểm tra:** từ gốc `mvn -q -DskipTests package`, **in mã thoát và đọc lỗi**, rồi mới deploy. Sau deploy, kiểm tra bằng `curl` các endpoint vừa đổi (và xem log service không có `ERROR`).
- **Bí mật:** chỉ qua biến môi trường (`.env` + `docker-compose.yml`); không đưa mật khẩu mặc định vào code ngoài giá trị dev trong `application.properties`.

<!-- END:stagio-rules -->

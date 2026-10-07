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

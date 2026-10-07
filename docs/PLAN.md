# Stagio — Kế hoạch và tiến độ (tài liệu sống)

> **Cập nhật lần cuối:** 2026-10-07 · bởi Cursor
> **Bản gốc duy nhất của file này:** `event-platform/docs/PLAN.md` (repo BE_Event_Platform, nhánh `master`).
> Các repo khác chỉ trỏ về đây, không giữ bản sao. Quy tắc làm việc ở `docs/rules/` (được đồng bộ sang từng repo thành `AGENTS.md` và `.cursor/rules/stagio.mdc`).

**Ai làm xong việc gì thì phải cập nhật file này ngay trong cùng lượt làm** (tick ô, thêm dòng vào mục 8 "Nhật ký", và ghi mục 0.2 nếu dừng giữa chừng). Chi tiết ở mục 9.

---

## 0. Đọc nhanh nếu bạn vừa tiếp quản

### 0.1 Việc tiếp theo (làm từ trên xuống, ưu tiên cao trước)

1. [ ] **Thử đầu cuối các luồng chưa kiểm tra** bằng tài khoản demo (xem `DEMO-ACCOUNTS.txt` ở `/Users/macos/Desktop/SaaS/`, không commit): đoàn đăng ảnh/video lên Khám phá, nén video và WebP, khách viết đánh giá, chat khách ↔ trưởng đoàn. Ghi kết quả vào mục 8.
2. [ ] **Tìm nguyên nhân thông báo đỏ "Không có quyền truy cập" (403) hiện ngay sau đăng nhập** (cả admin đoàn lẫn thành viên). Bật Dev (chạm 10 lần chân trang đăng nhập), mở con bọ debug, lấy đường dẫn bị 403. Xem mục 6.
3. [x] ~~Push hai nhánh app và backend, web lên GitHub~~ (xong 2026-10-07).
4. [ ] **Trang chi tiết đoàn**: ảnh bìa đoàn, dải show đã diễn của đoàn, điểm đánh giá của đoàn.
5. [ ] **Dựng production trên VPS FPT** (mục 5, Phase C) khi anh Nghĩa đưa IP, khoá SSH, tên miền và giá trị `.env`.

### 0.2 Đang làm dở (HANDOFF)

_Không có việc dở. Người dừng giữa chừng phải ghi vào đây: việc gì, đã làm đến đâu, file nào đang sửa dở, lệnh nào chạy dở, bước kế tiếp. Xoá mục này khi xong._

### 0.3 Cần anh Nghĩa quyết

- Hạn mức đẩy tin theo gói thuê bao: **bỏ ngỏ đến sau debut**. Mặc định vẫn 3 lượt / 24 giờ / đoàn. Chưa quyết có gắn theo gói hay không.

---

## 1. Sản phẩm và hướng kinh doanh

- **Stagio** (tên cũ Occasio): nền tảng cho **đoàn lân sư rồng** (và sau này các ngành sự kiện khác). Gốc là phần mềm quản lý đoàn, thêm sàn để khách thuê tìm và đặt show.
- **Hướng đi:** thu **phí thuê bao** theo tháng/năm có dùng thử (như KiotViet), không dựa vào hoa hồng. Sàn đặt show là kênh kéo khách. Đoàn **đăng bài miễn phí**; anh Nghĩa làm SEO và chạy quảng cáo hộ các đoàn.
- **Hai nhóm người dùng:** (1) đoàn (trưởng đoàn = ADMIN, thành viên = TN_MEMBER), (2) khách thuê (CUSTOMER). Thêm SUPER_ADMIN là chủ nền tảng.
- **Bản phát hành đầu trên Google Play chỉ có lân sư rồng**, nhẹ (~21MB), để qua đợt thử nghiệm kín 12 tester × 14 ngày mà không bị từ chối vì lỗi. Các ngành khác bật lại sau.
- **Trang chủ app = bảng tin các show đã diễn** (ảnh, video, mô tả, gói show, đánh giá). Tin tức là tính năng phụ.

## 2. Kiến trúc và môi trường

| Repo | Đường dẫn máy anh | Remote | Công nghệ |
| --- | --- | --- | --- |
| **event_app** (app) | `/Volumes/Code/event_app` | `nghiafullstack/event_platform_flutter` | Flutter, GetX, Dio |
| **event-platform** (backend) | `/Users/macos/Desktop/SaaS/event-platform` | `nghiafullstack/BE_Event_Platform` (`master`) | Java 17, Spring Boot 4, Maven nhiều module |
| **admin-web** (web) | `/Users/macos/Desktop/SaaS/admin-web` | `nghiafullstack/event_platform_web` | Next.js 16, React 19, TanStack Query, shadcn/ui |

**Dịch vụ backend** (qua `api-gateway` cổng 8080, domain `https://muong14.xyz`): `identity-service` 8081 (đăng nhập, đơn vị, user, xoá tài khoản), `catalog-service` 8082 (banner, bài viết, tải ảnh/video, hồ sơ vendor), `event-service` 8083 (show, gói, trưng bày, đánh giá, chat, sàn công khai), `customer-service` 8084 (khách, ghi chú), `notification-service` 8085 (FCM, hộp thư). Hạ tầng: MySQL ngoài (Aiven), Redis, RabbitMQ trong Docker Compose.

**Môi trường:**
- **Server dev/test hiện tại** (`muong14.xyz`, `rocky@124.197.20.141`, thư mục `~/BE_Event_Platform`): dùng chung với project khác của người khác, RAM chỉ ~750MB trống. App Dev và Production đều đang trỏ vào đây.
- **Production thật** sẽ dựng trên **VPS FPT riêng** (6 vCPU, 20GB RAM, 256GB SSD, 1 máy, Docker Compose, Nginx Proxy Manager). Chưa làm.
- Cấu hình app theo môi trường: `env/dev.json`, `env/prod.json`, chạy qua `./scripts/flutter_env.sh`; build bản phát hành qua `./scripts/build_release.sh apk|appbundle` (chỉ arm64, làm rối mã).

## 3. Nhánh và phát hành

| Repo | Nhánh | Vai trò |
| --- | --- | --- |
| event_app | `release/v1-lan-su-rong` | **Bản đang thử nghiệm kín trên Google Play, `1.0.0+3`, AAB 21,8MB.** Đóng băng, chỉ sửa lỗi nhỏ khi bắt buộc. |
| event_app | `release/v1.1-bang-tin` | **Bản cập nhật sau khi app được duyệt, `1.1.0+4`, AAB ~23,8MB.** Mọi tính năng mới làm ở đây. |
| event_app | `main` | Bản đầy đủ nhiều ngành, có thù lao/điểm công cho thành viên. Cơ sở để khôi phục sau đợt thử nghiệm. |
| event-platform | `master` | Duy nhất, API thêm chứ không đổi để cả hai bản app đều chạy được. |
| admin-web | `main` | Duy nhất. |

> **Bất biến:** không bao giờ upload bản v1.1 vào đợt thử nghiệm kín. Trước khi build, chạy `git branch --show-current`.

## 4. Hiện trạng theo module

Ký hiệu: ✅ xong và đã kiểm tra · 🟡 xong nhưng **chưa kiểm tra đầu cuối** · ⛔ chưa làm.

### 4.1 Backend (`master`)

| Mục | Trạng thái |
| --- | --- |
| Đăng nhập, đăng ký đơn vị, vai trò, xoá tài khoản, giới hạn tần suất | ✅ |
| Tối ưu hiệu năng (cache công khai, chỉ mục, timeout, N+1, JVM/mem limit) | ✅ |
| Mã show (`{mã đoàn}-{yyMM}-{số}`), ghi chú khách, show theo khách | ✅ (đã triển khai server dev) |
| Banner, bài viết (slug, SEO, làm sạch HTML), **đẩy tin** (3 lượt/24h) | ✅ |
| Tải ảnh/video: S3 FPT hoặc thư mục local (volume `catalog_uploads`), Range cho video | ✅ |
| **Nén video H.264 ~720p nền, ảnh → WebP** bằng ffmpeg trong container catalog | 🟡 lệnh ffmpeg đã thử trong container, luồng tải lên thật chưa thử |
| Show trưng bày: tiêu đề, mô tả, tối đa 10 mục (1 video đứng đầu + ảnh), cờ hiện công khai | ✅ API · 🟡 luồng app |
| Bảng tin công khai, chi tiết show có **gói của đoàn kèm `selected`**, **show liên quan** (±30% giá, cùng tỉnh; **chưa lọc phường** như quyết định 07/10) | 🟡 |
| **Đánh giá show** (1 khách 1 đánh giá, tên che `Ngu***`). Quyền ghi đã chốt 07/10 nhưng code vẫn cho mọi khách đăng nhập | ✅ đọc · 🟡 ghi · ⛔ chưa siết quyền |
| **Chat khách ↔ trưởng đoàn** gắn show, thông báo FCM | 🟡 |
| Dữ liệu demo (8 đoàn, gói, show, banner, bài, đánh giá, ảnh/video minh hoạ) bật bằng `DEMO_SEED_ENABLED` | ✅ trên server dev |
| **Thuê bao** (gói, dùng thử, thanh toán chuyển khoản, chặn 402 ở gateway) | ⛔ |
| Thu hồi/xoá file khi xoá show hay thay media | ⛔ (file cũ còn lại trong kho) |

### 4.2 App Flutter (`release/v1.1-bang-tin`)

| Mục | Trạng thái |
| --- | --- |
| Đổi tên Stagio, icon, splash, gói `com.tns.stagio`, ký bản phát hành, arm64 + làm rối mã | ✅ |
| Dev/Production chuyển mềm (chạm 10 lần chân trang), con bọ debug chỉ ở Dev | ✅ |
| Điều khoản, chính sách, xoá tài khoản trong app | ✅ |
| Trang chủ: banner có ảnh, khu vực, đơn vị, menu nhanh, bảng tin "Show vừa diễn" | ✅ (simulator iOS) |
| Chi tiết show: video/ảnh, gói đậm màu, đánh giá, show liên quan, thanh nút **Chat + Đặt show** cố định | ✅ hiển thị · 🟡 ghi đánh giá/chat |
| Đoàn đăng show lên Khám phá (chọn ảnh/video, tải lên) | 🟡 chưa chạy thật |
| Tin tức, bài viết của đoàn, đẩy tin | ✅ hiển thị · 🟡 đẩy tin |
| Hộp thư chat (khách và trưởng đoàn) | 🟡 |
| Đã kiểm tra trên **Android thật** các tính năng mới | ⛔ chưa (chỉ iOS simulator) |

### 4.3 Web (`main`)

Đã có: trang công khai (Trang chủ, Tin tức, Đăng ký, Điều khoản, Chính sách, Xoá tài khoản), khu Super Admin (dashboard, banner, danh mục, bài viết, đơn vị), khu đơn vị `host/*` (dashboard, show, khách, thành viên, gói, vị trí, tài chính, cài đặt, bài viết).
**Chưa có so với app/backend mới:** màn trưng bày show (ảnh/video/mô tả), hộp thư chat, đánh giá, đẩy tin, trang thuê bao. **Chưa triển khai công khai** (cần cho đường dẫn chính sách bảo mật và trang xoá tài khoản của Google Play).

## 5. Lộ trình

### Phase A — Đợt thử nghiệm kín Google Play (đến khoảng 20/10/2026)
- [x] Tạo app, gói `com.tns.stagio`, bản `1.0.0+3` (21,8MB) lên kênh thử nghiệm.
- [ ] Đủ 12 tester, đủ 14 ngày liên tục.
- [ ] Điền hồ sơ cửa hàng: Data safety (bản **1.1 thêm "Ảnh và video"**), phân loại nội dung, mục tiêu độ tuổi.
- [ ] Điền chỗ `[CẦN CẬP NHẬT: …]` trong điều khoản/chính sách (tên công ty, email, địa chỉ, hotline), xuất lại JSON cho app (`admin-web/scripts/export-legal.mjs`).
- [ ] Tài khoản demo cho người duyệt (đoàn demo trong `DEMO-ACCOUNTS.txt`).
- [ ] Hạn chế khoá Google Maps/Goong theo gói và chữ ký.

### Phase B — Hoàn thiện bản 1.1 (nhánh `v1.1-bang-tin`)
- [x] Bảng tin show, chi tiết show, đánh giá, show liên quan, chat, đẩy tin, thanh nút cố định.
- [ ] Siết quyền đánh giá: chỉ khách đã dùng đúng show đó và đã từng dùng show của đơn vị đó.
- [ ] Show liên quan lọc thêm **cùng phường** (hiện chỉ cùng tỉnh), giữ ±30% giá và tối đa 6.
- [ ] Thử đầu cuối các luồng 🟡 ở mục 4 trên iOS **và Android thật**.
- [ ] Trang chi tiết đoàn đẹp hơn (ảnh bìa, show đã diễn, điểm đánh giá).
- [ ] Bấm thông báo chat mở đúng cuộc trò chuyện; chat gửi ảnh.
- [ ] Sửa lỗi 403 sau đăng nhập (mục 6).
- [ ] Cập nhật In-App Update + kiểm tra phiên bản tối thiểu từ server; cân nhắc Shorebird (code push).
- [ ] Build AAB `1.1.0+4`, đo dung lượng, thử trên máy thật, đẩy lên Play sau khi bản v1 được duyệt.

### Phase C — Production trên VPS FPT (làm cuối tuần khi có VPS)
- [ ] Nhận IP, khoá SSH, tên miền, giá trị `.env` (JWT mới, Super Admin, S3 FPT, Firebase, SMTP).
- [ ] Docker Compose một VM; Nginx Proxy Manager (cổng 81 chỉ cho IP anh), HTTPS, chỉ mở 80/443.
- [ ] MySQL/Redis/RabbitMQ chỉ mạng nội bộ, có mật khẩu; **không bật `DEMO_SEED_ENABLED`** (hoặc bật có chủ đích).
- [ ] `mem_limit` từng service; catalog cần ~640MB vì ffmpeg. Kiểm tra giới hạn body nginx cho video ~100MB.
- [ ] Sao lưu MySQL hằng đêm lên S3, giám sát, cảnh báo.
- [ ] Cấu hình S3 FPT cho ảnh/video để lưu bền (hiện đang local).
- [ ] Cookie `Secure` cho web phía sau TLS; JDBC của Aiven bật SSL.
- [ ] Đổi `API_BASE_URL` và `env/prod.json` sang domain production.

### Phase D — Web công khai và quản trị
- [ ] Triển khai web (SSR) lên domain; trỏ chính sách bảo mật và trang xoá tài khoản.
- [ ] Màn trưng bày show, hộp thư chat, đánh giá, đẩy tin trên web `host/*`.
- [ ] SEO: sitemap, robots, JSON-LD, Search Console; đoàn tự cấu hình SEO bài viết.

### Phase E — Thuê bao (mô hình KiotViet)
- [ ] Bảng gói, dùng thử 14 ngày lúc đăng ký đơn vị, yêu cầu thanh toán với mã chuyển khoản, Super Admin xác nhận.
- [ ] Gateway chặn thao tác ghi khi hết hạn: trả **402 `SUBSCRIPTION_EXPIRED`**, vẫn cho đọc.
- [ ] Ẩn đơn vị hết hạn khỏi sàn; app hiển thị thông báo gọn khi gặp 402.
- [ ] Sau đó nối SePay (webhook), email/FCM nhắc hết hạn. Gắn hạn mức đẩy tin theo gói.

### Phase F — Sau đợt thử nghiệm
- [ ] Bỏ `kOnlyCategory` để mở lại các ngành khác; khôi phục màn thành viên, thù lao, điểm công từ `main` có chọn lọc.
- [ ] Crashlytics.
- [ ] Bản iOS lên App Store.

## 6. Nợ kỹ thuật và lỗi đã biết

| Vấn đề | Chi tiết | Hướng xử lý |
| --- | --- | --- |
| **403 "Không có quyền truy cập" sau đăng nhập** | Hiện thông báo đỏ, app vẫn dùng được; gặp với cả admin đoàn và thành viên. Log gateway/service không thấy dòng 403. Chưa biết endpoint nào. | Bật Dev, xem con bọ debug để lấy URL; hoặc thêm log vào `ErrorInterceptor` tạm thời. |
| Hết thời gian chờ gateway 30s | Tải video lớn qua gateway có thể chạm giới hạn | Theo dõi khi thử video thật; nâng timeout riêng cho `/api/files/**` nếu cần. |
| Dung lượng đĩa/RAM server dev | RAM trống ~750MB, dùng chung với project khác | Không chạy tác vụ nặng liên tục; nén video chạy một cái một lúc. |
| Aiven JDBC `useSSL=false` | Dữ liệu đi không mã hoá tới DB | Bật SSL khi lên production. |
| Chat chỉ có chữ, thăm dò 4 giây | Chưa có WebSocket, chưa gửi ảnh | Chấp nhận cho bản đầu. |
| File cũ không bị xoá | Thay/gỡ ảnh video của show không dọn khỏi kho | Làm job dọn file mồ côi. |
| Ảnh/video demo là hình tự vẽ | Không phải ảnh thật | Đoàn tự đăng ảnh thật; xoá dữ liệu demo khi vào production. |

## 7. Quyết định đã chốt (đừng lật lại nếu chưa hỏi anh Nghĩa)

1. Giữ Java/Spring Boot microservices, không viết lại. Gateway là cổng duy nhất; gọi nội bộ có `X-Internal-Token`.
2. Bản phát hành đầu **chỉ lân sư rồng**, gọn; ngành khác bật lại sau đợt thử nghiệm.
3. Doanh thu chính là **thuê bao**; đăng bài **miễn phí**, không thu phí đăng bài.
4. Gia hạn thuê bao ban đầu bằng **chuyển khoản, Super Admin xác nhận**, sau mới nối SePay.
5. Tin tức là **tính năng phụ**; trang chủ app là bảng tin show.
6. Show chỉ lên Khám phá khi **đoàn chủ động đăng**; mặc định không hiện. API công khai **không** trả địa chỉ, khách, số tiền, mã show.
7. Mỗi show tối đa **10 mục: 1 video và 9 ảnh, hoặc 10 ảnh**, video luôn đứng đầu. Video nén phía server; ảnh chuyển WebP.
8. Show liên quan: giá gói trong **±30%**, **cùng tỉnh/thành và cùng phường** của đoàn, tối đa 6. Không dùng khoảng cách từ toạ độ show.
9. Đẩy tin: mặc định **3 lượt/24 giờ/đoàn**, mỗi bài chỉ đẩy lại sau 24 giờ. Gắn hạn mức theo gói thuê bao **để sau debut, chưa chốt**.
10. Bảng tin và trang chi tiết lấy dữ liệu công khai, không cần đăng nhập. Chat cần đăng nhập khách. Đánh giá chỉ cho khách **đã dùng đúng show đó và đã từng dùng show của đơn vị đó** (đăng nhập thôi thì không đủ).
11. Biểu mẫu trong app dùng **bottom sheet**, không dùng hộp thoại giữa màn.
12. Không thêm thư viện nặng vào app khi chưa cân nhắc dung lượng (đích ~24MB).
13. Chỉ thao tác trong `~/BE_Event_Platform` trên server dùng chung; không đụng project khác.

## 8. Nhật ký tiến độ (mới nhất trên cùng)

- **2026-10-07** · Chốt: đánh giá chỉ cho khách đã dùng đúng show và đã từng dùng show của đơn vị; show liên quan theo tỉnh và phường; đẩy tin giữ 3/24h, gắn gói thuê bao bỏ ngỏ sau debut. Chưa sửa code.
- **2026-10-07** · Push lên GitHub: backend `master`, web `main`, app `release/v1-lan-su-rong` và `release/v1.1-bang-tin` (không force).
- **2026-10-07** · Lập bản kế hoạch và quy tắc chung này; đồng bộ sang 3 repo (`AGENTS.md`, `CLAUDE.md`, `.cursor/rules/stagio.mdc`). Archive kế hoạch cũ 17/9.
- **2026-10-07** · Chi tiết show: gói của đoàn (gói đã dùng đậm), đánh giá, show liên quan, Chat + Đặt show cạnh nhau; media tối đa 10 (1 video đứng đầu); nén video và WebP bằng ffmpeg. Backend commit `master`; app commit `2982c81` trên `release/v1.1-bang-tin`.
- **2026-10-07** · Chat khách ↔ trưởng đoàn, trang chủ sắp lại (khu vực + đơn vị lên đầu, bỏ mục gói), thanh nút cố định ở chi tiết show.
- **2026-10-07** · Bảng tin show (ảnh, video phát trong app, mô tả, gói), đoàn đăng show, ảnh minh hoạ demo, video Range, tách nhánh `v1` (test) và `v1.1`.
- **2026-10-06** · Dữ liệu demo 8 đoàn lên server dev; tin tức và đẩy tin; bản AAB `1.0.0+3` ký bằng khoá upload; chuyển dòng điều khoản xuống dưới nút đăng nhập.
- **Trước đó** · Tách nhánh lean `v1-lan-su-rong`, tối ưu backend, đóng cổng Redis/RabbitMQ, web công khai + SEO + pháp lý, tên Stagio, Dev/Prod mềm, báo giá FPT.

## 9. Cách cập nhật tài liệu này

1. **Trước khi làm:** đọc mục 0 và mục liên quan. Nếu thấy mục 0.2 có việc dở, làm nốt việc đó trước.
2. **Làm xong một việc:** tick `[x]` đúng dòng trong mục 5, sửa trạng thái ở mục 4 (✅/🟡/⛔), thêm **một dòng** vào mục 8 (ngày, việc, commit hoặc file chính), cập nhật dòng "Cập nhật lần cuối" ở đầu file.
3. **Sắp hết lượt hoặc phải dừng giữa chừng:** ghi vào mục 0.2: việc gì, đã làm đến đâu, file nào còn dở, lệnh nào chạy dở, bước kế tiếp cụ thể. Không để code dở mà không ghi.
4. **Phát hiện lỗi hoặc nợ mới:** thêm vào mục 6. **Có quyết định mới:** thêm vào mục 7 (và hỏi anh Nghĩa nếu nó lật quyết định cũ).
5. Giữ tài liệu **ngắn và đúng**: xoá mục đã xong và không còn ý nghĩa, đừng chép code vào đây.
6. Commit thay đổi plan **cùng commit** với việc vừa làm (repo `event-platform`, tiền tố `docs:` nếu chỉ sửa tài liệu). Nếu việc nằm ở repo khác, commit plan riêng ở `event-platform`.

## Riêng cho web (`/Users/macos/Desktop/SaaS/admin-web`)

- **Đây là Next.js 16 / React 19, có thay đổi phá vỡ so với bản cũ.** Đọc hướng dẫn trong `node_modules/next/dist/docs/` trước khi viết code mới và để ý các cảnh báo ngừng hỗ trợ. (Khối `nextjs-agent-rules` trong `AGENTS.md` do `next dev` tự sinh, giữ nguyên.)
- **Cấu trúc:** nhóm route `(public)` (trang công khai, SEO), `admin/*` (Super Admin), `host/*` (đơn vị), `login`; logic theo vùng ở `src/features/{admin,host,public,share}`; gọi API qua `src/common/fetch` (server) và BFF `/api/backend/[...path]`; chạy chặn quyền ở `src/proxy.ts`.
- **Gọi API:** dùng wrapper `fetch` có sẵn, **không dùng axios**. Token nằm trong cookie **httpOnly** (`token`, `refresh_token`, `role`); không đưa token ra JavaScript phía trình duyệt, không đặt bí mật vào biến `NEXT_PUBLIC_*`.
- **Thư viện đã chọn:** TanStack Query (dữ liệu phía client), shadcn/ui + Tailwind 4, react-hook-form + zod, Tiptap v3 (soạn bài), recharts. Đừng thêm thư viện cùng chức năng.
- **SEO cho trang công khai:** render phía server, có `generateMetadata` (title, description, canonical, Open Graph), JSON-LD, `revalidate` ngắn; bài nháp/ẩn không xuất hiện ở trang công khai, sitemap.
- **Pháp lý:** nội dung điều khoản/chính sách nằm ở `src/content/legal/*.json`; sửa xong chạy `scripts/export-legal.mjs` để cập nhật bản trong app.
- **Giao diện:** tiếng Việt, responsive tối thiểu ở 390px và 1440px; biểu mẫu dài dùng hộp trượt hoặc trang riêng chứ không nhồi vào hộp thoại.
- **Kiểm tra trước khi báo xong:** `npm run lint` và `npm run build` không lỗi; xem thử các trang vừa sửa ở hai kích thước màn hình.
- **Tính năng mới của app/backend phải có phần tương ứng trên web** (hoặc ghi vào PLAN.md mục 5 Phase D là chưa làm).

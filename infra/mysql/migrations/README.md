# SQL đổi cấu trúc cho production

Production chạy `JPA_DDL_AUTO=none` bằng user `admin` không có quyền CREATE/ALTER/DROP, nên Hibernate không tự thêm bảng hay cột.
Mỗi thay đổi cấu trúc (bảng, cột, chỉ mục) có một file `YYYY-MM-DD-mo-ta.sql` ở đây, ghi rõ schema (`USE event_db;`), cột mới nullable hoặc có giá trị mặc định.
Chạy bằng root trên production **trước khi** deploy code tương ứng, rồi ghi vào `docs/PLAN.md`. Dev vẫn để `update` nên không cần chạy.

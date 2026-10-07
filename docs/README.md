# Tài liệu dự án Stagio

| File | Dùng để làm gì |
| --- | --- |
| `PLAN.md` | **Kế hoạch và tiến độ sống.** Bản gốc duy nhất. Mọi người và mọi AI đọc trước khi làm và cập nhật khi làm xong hoặc phải dừng. |
| `rules/common.md` | Quy tắc chung cho cả 3 repo. |
| `rules/app.md`, `rules/backend.md`, `rules/web.md` | Quy tắc riêng từng repo. |
| `archive/` | Kế hoạch cũ, chỉ để tham khảo, không còn đúng. |

## Quy tắc được các công cụ đọc thế nào

Chạy `scripts/sync-agent-rules.sh` sau khi sửa `rules/*.md`. Script ghi vào mỗi repo (`event_app`, `admin-web`, `event-platform`):

- `AGENTS.md` (khối `stagio-rules`): Cursor và nhiều công cụ AI đọc file này;
- `CLAUDE.md` (dòng `@AGENTS.md`): Claude Code đọc;
- `.cursor/rules/stagio.mdc` (`alwaysApply`): Cursor.

Chỉ sửa ở `docs/rules/`, không sửa tay các file sinh ra vì lần đồng bộ sau sẽ ghi đè khối của mình (phần khác trong `AGENTS.md`, như khối Next.js tự sinh, vẫn được giữ).

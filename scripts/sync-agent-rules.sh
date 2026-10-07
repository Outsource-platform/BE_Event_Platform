#!/usr/bin/env bash
# Đồng bộ quy tắc từ docs/rules/*.md sang 3 repo (app, backend, web) cho cả Claude Code lẫn Cursor.
#   - <repo>/AGENTS.md              khối giữa BEGIN/END:stagio-rules (phần khác trong file giữ nguyên)
#   - <repo>/CLAUDE.md              chỉ cần có dòng @AGENTS.md
#   - <repo>/.cursor/rules/stagio.mdc   luôn bật (alwaysApply)
# Sửa quy tắc ở docs/rules/ rồi chạy lại script này. Không sửa tay các file sinh ra.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_REPO="${APP_REPO:-/Volumes/Code/event_app}"
WEB_REPO="${WEB_REPO:-/Users/macos/Desktop/SaaS/admin-web}"
BACKEND_REPO="${BACKEND_REPO:-$ROOT}"

python3 - "$ROOT" "$APP_REPO" "$WEB_REPO" "$BACKEND_REPO" <<'PY'
import os, sys

root, app, web, backend = sys.argv[1:5]
rules = os.path.join(root, "docs", "rules")
read = lambda n: open(os.path.join(rules, n), encoding="utf-8").read().strip()

PLAN = os.path.join(root, "docs", "PLAN.md")
HEADER = f"""# Stagio — quy tắc làm việc (tự sinh từ `docs/rules/`, đừng sửa tay)

> **Kế hoạch và tiến độ sống:** `{PLAN}`
> Đọc file đó trước khi làm, cập nhật nó trước khi kết thúc lượt. Quy tắc gốc: `{os.path.join(root, 'docs', 'rules')}/`.
"""

targets = {app: "app.md", web: "web.md", backend: "backend.md"}
BEGIN, END = "<!-- BEGIN:stagio-rules -->", "<!-- END:stagio-rules -->"

for repo, specific in targets.items():
    if not os.path.isdir(repo):
        print(f"bỏ qua (không thấy thư mục): {repo}")
        continue
    body = "\n\n".join([HEADER.strip(), read("common.md"), read(specific)]) + "\n"
    block = f"{BEGIN}\n\n{body}\n{END}\n"

    # AGENTS.md: thay khối của mình, giữ nguyên mọi thứ khác (vd khối quy tắc Next.js tự sinh).
    path = os.path.join(repo, "AGENTS.md")
    current = open(path, encoding="utf-8").read() if os.path.exists(path) else ""
    if BEGIN in current and END in current:
        head, rest = current.split(BEGIN, 1)
        _, tail = rest.split(END, 1)
        new = head + block + tail.lstrip("\n")
    else:
        new = block + ("\n" + current if current else "")
    open(path, "w", encoding="utf-8").write(new)

    # CLAUDE.md: Claude Code đọc file này; trỏ về AGENTS.md để chỉ có một bản nội dung.
    path = os.path.join(repo, "CLAUDE.md")
    current = open(path, encoding="utf-8").read() if os.path.exists(path) else ""
    if "@AGENTS.md" not in current:
        open(path, "w", encoding="utf-8").write((current.rstrip() + "\n\n" if current.strip() else "") + "@AGENTS.md\n")

    # Cursor: quy tắc luôn bật.
    cdir = os.path.join(repo, ".cursor", "rules")
    os.makedirs(cdir, exist_ok=True)
    front = "---\ndescription: Quy tắc và kế hoạch chung của dự án Stagio (app, backend, web)\nalwaysApply: true\n---\n\n"
    open(os.path.join(cdir, "stagio.mdc"), "w", encoding="utf-8").write(front + body)
    print(f"đã đồng bộ: {repo}")
PY

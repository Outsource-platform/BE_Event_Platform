-- Giới hạn số thành viên nhận show. NULL = không giới hạn (show cũ giữ nguyên hành vi).
ALTER TABLE events ADD COLUMN member_limit INT NULL;

-- Báo cáo nội dung xấu và chặn trong chat (quy định 1.2 của App Store), backend event-service.
-- Chạy bằng root trên production TRƯỚC khi deploy event-service bản có ContentReport. Chạy lại không lỗi.
USE event_db;

CREATE TABLE IF NOT EXISTS content_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporter_user_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    reason VARCHAR(20) NOT NULL,
    note VARCHAR(500) NULL,
    snapshot VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    handled_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    is_deleted BIT(1) NOT NULL DEFAULT b'0',
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_report_status (status, created_at),
    KEY idx_report_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Cột mới có giá trị mặc định để cuộc trò chuyện cũ không hỏng.
SET @has := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = 'event_db' AND table_name = 'chat_conversations' AND column_name = 'customer_blocked');
SET @sql := IF(@has = 0,
    'ALTER TABLE chat_conversations ADD COLUMN customer_blocked BIT(1) NOT NULL DEFAULT b''0'', ADD COLUMN troupe_blocked BIT(1) NOT NULL DEFAULT b''0''',
    'SELECT ''chat_conversations đã có cột chặn''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

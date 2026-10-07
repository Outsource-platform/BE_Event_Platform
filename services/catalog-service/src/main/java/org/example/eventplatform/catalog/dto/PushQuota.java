package org.example.eventplatform.catalog.dto;

import java.time.LocalDateTime;

/** Hạn mức đẩy tin của đơn vị: đã dùng bao nhiêu lượt, và khi nào lượt sớm nhất được trả lại. */
public record PushQuota(int limit, int used, LocalDateTime resetsAt) {
}

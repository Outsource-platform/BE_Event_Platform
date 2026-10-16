package org.example.eventplatform.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public final class ReportDtos {

    private ReportDtos() {
    }

    @Getter
    @Setter
    public static class CreateRequest {
        @NotBlank(message = "Thiếu loại nội dung")
        private String targetType;
        @NotNull(message = "Thiếu nội dung cần báo cáo")
        private Long targetId;
        @NotBlank(message = "Chọn lý do báo cáo")
        private String reason;
        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        private String note;
    }

    @Getter
    @Setter
    public static class HandleRequest {
        /** REMOVED: gỡ nội dung; DISMISSED: bỏ qua. */
        @NotBlank(message = "Chọn cách xử lý")
        private String status;
    }

    public record Item(Long id, Long reporterUserId, String targetType, Long targetId, String reason, String note,
                       String snapshot, String status, LocalDateTime createdAt, LocalDateTime handledAt) {
    }

    public record Page(List<Item> items, int page, int size, long totalElements, int totalPages) {
    }
}

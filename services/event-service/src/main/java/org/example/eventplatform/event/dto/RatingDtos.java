package org.example.eventplatform.event.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class RatingDtos {

    private RatingDtos() {
    }

    @Getter
    @Setter
    public static class Request {
        @Min(value = 1, message = "Chọn từ 1 đến 5 sao")
        @Max(value = 5, message = "Chọn từ 1 đến 5 sao")
        private int stars;

        @Size(max = 500, message = "Nhận xét tối đa 500 ký tự")
        private String comment;
    }

    /** [distribution] có đủ khoá 1..5 để app vẽ biểu đồ không phải tự điền số 0. */
    public record Summary(double average, int total, Map<Integer, Integer> distribution) {
    }

    public record Item(Long id, String userName, int stars, String comment, LocalDateTime createdAt) {
    }

    public record Page(Summary summary, List<Item> items, int page, int totalPages, long totalElements) {
    }
}

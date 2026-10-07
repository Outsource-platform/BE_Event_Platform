package org.example.eventplatform.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Nội dung trưng bày một show: tiêu đề, mô tả và danh sách ảnh/video theo thứ tự hiển thị. */
@Getter
@Setter
public class ShowcaseRequest {

    private boolean published;

    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự")
    private String description;

    @Valid
    @Size(max = 10, message = "Tối đa 10 ảnh và video cho mỗi show")
    private List<MediaItem> media;

    @Getter
    @Setter
    public static class MediaItem {
        @NotBlank
        private String type; // IMAGE hoặc VIDEO

        @NotBlank
        @Size(max = 600)
        private String url;
    }
}

package org.example.eventplatform.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.example.eventplatform.catalog.entity.PostStatus;

@Getter
@Setter
public class PostRequest {

    @NotBlank(message = "Tiêu đề bài viết không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    // Để trống thì tự sinh từ tiêu đề
    @Size(max = 200, message = "Slug tối đa 200 ký tự")
    private String slug;

    @Size(max = 500, message = "Mô tả ngắn tối đa 500 ký tự")
    private String excerpt;

    private String content;

    private String coverImage;

    @Size(max = 255, message = "Tiêu đề SEO tối đa 255 ký tự")
    private String seoTitle;

    @Size(max = 500, message = "Mô tả SEO tối đa 500 ký tự")
    private String seoDescription;

    private PostStatus status;
}

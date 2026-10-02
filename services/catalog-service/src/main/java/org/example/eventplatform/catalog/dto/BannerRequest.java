package org.example.eventplatform.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BannerRequest {

    @NotBlank(message = "Tiêu đề banner không được để trống")
    private String title;

    private String subtitle;

    private String imageUrl;

    private String linkUrl;

    private Integer sortOrder;

    private Boolean active;
}

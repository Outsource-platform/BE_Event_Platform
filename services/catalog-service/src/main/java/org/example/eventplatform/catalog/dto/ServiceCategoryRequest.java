package org.example.eventplatform.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceCategoryRequest {

    @NotBlank(message = "Tên loại dịch vụ không được để trống")
    private String name;

    @NotBlank(message = "Code không được để trống")
    private String code;

    private String description;

    // Để trống khi tạo (mặc định bật); khi sửa dùng để ẩn/hiện lại loại dịch vụ.
    private Boolean active;
}

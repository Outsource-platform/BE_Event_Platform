package org.example.eventplatform.identity.dto.tenant;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTenantThemeRequest {

    // Gửi null để xoá override, quay lại màu mặc định theo hạng mục.
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Màu chính phải dạng #RRGGBB")
    private String primaryColorHex;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Màu nhấn phải dạng #RRGGBB")
    private String accentColorHex;

    // Tỉnh/thành đơn vị hoạt động, hiện trong phần khám phá khu vực của khách.
    private String province;

    // Phường/xã trong tỉnh đã chọn; gửi chuỗi rỗng để xoá.
    private String ward;
}

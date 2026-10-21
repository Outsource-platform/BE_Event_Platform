package org.example.eventplatform.identity.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    // Cách cũ (app v1, web): mã đoàn + username. App mới gửi phone thay cho cả hai.
    private String username;

    /** Đăng nhập bằng số điện thoại (app từ 1.1.0+11): không cần mã đoàn, server tự biết vai trò và đoàn. */
    private String phone;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    // Mã đoàn (Tenant.domain). Để trống = đăng nhập tài khoản Super Admin (không thuộc tenant nào).
    private String tenantDomain;
}

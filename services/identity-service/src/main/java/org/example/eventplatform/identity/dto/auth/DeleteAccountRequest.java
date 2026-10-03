package org.example.eventplatform.identity.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteAccountRequest {

    @NotBlank(message = "Nhập mật khẩu để xác nhận xoá tài khoản")
    private String password;
}

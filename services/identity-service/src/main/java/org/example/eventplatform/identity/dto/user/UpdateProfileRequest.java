package org.example.eventplatform.identity.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Trưởng đoàn sửa thông tin cơ bản của thành viên. [newPassword] để trống là giữ mật khẩu cũ. */
@Getter
@Setter
public class UpdateProfileRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 120, message = "Họ tên tối đa 120 ký tự")
    private String fullName;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @Email(message = "Email không hợp lệ")
    private String email;

    @Size(min = 6, max = 72, message = "Mật khẩu mới từ 6 đến 72 ký tự")
    private String newPassword;
}

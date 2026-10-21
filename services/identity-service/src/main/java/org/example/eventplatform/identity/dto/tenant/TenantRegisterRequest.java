package org.example.eventplatform.identity.dto.tenant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TenantRegisterRequest {

    @NotBlank(message = "Tên đơn vị không được để trống")
    @Size(max = 255, message = "Tên quá dài")
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mã đoàn không được để trống")
    @Size(min = 3, max = 30, message = "Mã đoàn phải từ 3 đến 30 ký tự")
    @Pattern(regexp = "^[a-z0-9]+$", message = "Mã đoàn chỉ gồm chữ thường và số, không dấu, không khoảng trắng, không gạch nối (VD: doanlandainam)")
    private String domain;

    @NotBlank(message = "Hạng mục sự kiện không được để trống")
    @Pattern(regexp = "^(LION_DANCE|GENERAL)$", message = "Hạng mục sự kiện không hợp lệ")
    private String category;

    @NotBlank(message = "Username cho admin không được để trống")
    private String adminUsername;

    @NotBlank(message = "Mật khẩu cho admin không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String adminPassword;

    /** SĐT trưởng đoàn, dùng để đăng nhập app. Không bắt buộc để app v1 (còn form đăng ký đoàn) vẫn gửi được. */
    private String adminPhone;
}

package org.example.eventplatform.identity.service;

import lombok.RequiredArgsConstructor;
import org.example.eventplatform.identity.entity.User;
import org.example.eventplatform.identity.entity.UserStatus;
import org.example.eventplatform.identity.repository.UserRepository;
import org.example.eventplatform.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Số điện thoại là tên đăng nhập trên app (khách, trưởng đoàn, thành viên đều đăng nhập bằng SĐT + mật khẩu),
 * nên mỗi SĐT chỉ thuộc một tài khoản còn hoạt động trên toàn hệ thống.
 */
@Component
@RequiredArgsConstructor
public class PhoneAccounts {

    private final UserRepository userRepository;

    /** "+84 901 234 567", "84901234567", "0901.234.567" → "0901234567"; trống → null; sai dạng → lỗi PHONE_INVALID. */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+84")) {
            digits = "0" + digits.substring(3);
        } else if (digits.startsWith("84") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        if (!digits.matches("^0\\d{9}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PHONE_INVALID", "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0");
        }
        return digits;
    }

    /** Tài khoản còn hoạt động (chưa xoá) đang dùng SĐT này; khách cũ lưu SĐT ở username nên tra cả hai. */
    public List<User> findActive(String phone) {
        return userRepository.findAllByPhoneOrCustomerUsername(phone).stream()
                .filter(u -> u.getStatus() != UserStatus.DELETED)
                .toList();
    }

    /** Báo lỗi PHONE_TAKEN nếu SĐT đã thuộc tài khoản khác ({@code exceptUserId} là chính người đang sửa). */
    public void ensureFree(String phone, Long exceptUserId) {
        if (phone == null) {
            return;
        }
        boolean taken = findActive(phone).stream().anyMatch(u -> !u.getId().equals(exceptUserId));
        if (taken) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_TAKEN", "Số điện thoại này đã được dùng cho một tài khoản khác");
        }
    }
}

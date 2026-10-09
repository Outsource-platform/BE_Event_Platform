package org.example.eventplatform.identity.crypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mã hoá số tài khoản và tên chủ tài khoản còn lưu chữ thường từ trước khi có mã hoá. Chạy mỗi lần khởi động và không làm gì
 * khi đã mã hoá hết. Mỗi giá trị được giải mã ngược để so khớp rồi mới ghi, tránh ghi một bản mã hỏng.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class BankAccountEncryptionMigration implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, bank_account_number, bank_account_holder FROM users "
                        + "WHERE (bank_account_number IS NOT NULL AND bank_account_number NOT LIKE 'enc:v1:%') "
                        + "OR (bank_account_holder IS NOT NULL AND bank_account_holder NOT LIKE 'enc:v1:%')");
        for (Map<String, Object> row : rows) {
            String number = seal((String) row.get("bank_account_number"));
            String holder = seal((String) row.get("bank_account_holder"));
            jdbc.update("UPDATE users SET bank_account_number = ?, bank_account_holder = ? WHERE id = ?",
                    number, holder, row.get("id"));
        }
        if (!rows.isEmpty()) {
            log.info("Đã mã hoá tài khoản ngân hàng của {} người dùng", rows.size());
        }
    }

    private static String seal(String value) {
        if (value == null || FieldEncryptor.isEncrypted(value)) {
            return value;
        }
        String sealed = FieldEncryptor.encrypt(value);
        if (!value.equals(FieldEncryptor.decrypt(sealed))) {
            throw new IllegalStateException("Mã hoá thử không giải mã ngược khớp, dừng để không làm hỏng dữ liệu");
        }
        return sealed;
    }
}

package org.example.eventplatform.identity.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Mã hoá một trường chữ trước khi ghi xuống CSDL (AES-256-GCM, mỗi lần một IV ngẫu nhiên). Dùng cho dữ liệu nhạy cảm
 * như số tài khoản ngân hàng: lộ bản sao CSDL cũng không đọc được nếu không có khoá {@code DATA_ENCRYPTION_KEY}.
 *
 * <p>Dạng lưu: {@code enc:v1:<base64(IV + bản mã + thẻ xác thực)>}. Giá trị không có tiền tố là dữ liệu cũ chưa mã hoá,
 * vẫn đọc được để không hỏng bản ghi cũ trong lúc chuyển đổi.
 *
 * <p>Thiếu khoá hoặc khoá sai độ dài thì service không khởi động: thà dừng còn hơn ghi dữ liệu nhạy cảm dạng chữ thường.
 * <b>Mất khoá là mất dữ liệu đã mã hoá</b>, nên khoá phải được sao lưu riêng.
 */
@Component
public class FieldEncryptor {

    public static final String PREFIX = "enc:v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static volatile SecretKeySpec key;

    public FieldEncryptor(@Value("${security.data-key:}") String base64Key) {
        init(base64Key);
    }

    /** Tách ra để kiểm thử được mà không cần dựng Spring. */
    public static void init(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("Thiếu DATA_ENCRYPTION_KEY: tạo bằng `openssl rand -base64 32` và khai trong .env");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY phải là chuỗi base64", e);
        }
        if (raw.length != 32) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY phải là 32 byte (base64 của `openssl rand -base64 32`)");
        }
        key = new SecretKeySpec(raw, "AES");
    }

    public static boolean isEncrypted(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    public static String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] sealed = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + sealed.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(sealed, 0, out, iv.length, sealed.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không mã hoá được dữ liệu", e);
        }
    }

    /** Giá trị chưa có tiền tố (dữ liệu cũ) được trả nguyên. */
    public static String decrypt(String stored) {
        if (stored == null || !isEncrypted(stored)) {
            return stored;
        }
        try {
            byte[] in = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, in, 0, IV_BYTES));
            return new String(cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // Sai khoá hoặc bản mã bị sửa: không đoán, không trả chuỗi rác.
            throw new IllegalStateException("Không giải mã được dữ liệu (sai DATA_ENCRYPTION_KEY hoặc dữ liệu bị sửa)", e);
        }
    }

    private static SecretKeySpec requireKey() {
        SecretKeySpec k = key;
        if (k == null) {
            throw new IllegalStateException("Chưa khởi tạo khoá mã hoá dữ liệu");
        }
        return k;
    }
}

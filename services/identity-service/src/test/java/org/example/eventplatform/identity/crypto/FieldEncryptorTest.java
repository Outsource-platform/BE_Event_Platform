package org.example.eventplatform.identity.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class FieldEncryptorTest {

    private static String newKey() {
        byte[] k = new byte[32];
        new SecureRandom().nextBytes(k);
        return Base64.getEncoder().encodeToString(k);
    }

    @BeforeEach
    void setUp() {
        FieldEncryptor.init(newKey());
    }

    @Test
    void roundTripGiữNguyênChữCóDấu() {
        String plain = "NGUYỄN TRỌNG NGHĨA 0123456789";
        String sealed = FieldEncryptor.encrypt(plain);
        assertTrue(sealed.startsWith(FieldEncryptor.PREFIX));
        assertFalse(sealed.contains("0123456789"));
        assertEquals(plain, FieldEncryptor.decrypt(sealed));
    }

    @Test
    void mỗiLầnMãHoáRaBảnMãKhácNhau() {
        assertNotEquals(FieldEncryptor.encrypt("123456789"), FieldEncryptor.encrypt("123456789"));
    }

    @Test
    void dữLiệuCũChưaMãHoáVẫnĐọcĐược() {
        assertEquals("123456789", FieldEncryptor.decrypt("123456789"));
        assertNull(FieldEncryptor.decrypt(null));
        assertNull(FieldEncryptor.encrypt(null));
    }

    @Test
    void saiKhoáHoặcBảnMãBịSửaThìBáoLỗi() {
        String sealed = FieldEncryptor.encrypt("123456789");
        FieldEncryptor.init(newKey());
        assertThrows(IllegalStateException.class, () -> FieldEncryptor.decrypt(sealed));

        String own = FieldEncryptor.encrypt("123456789");
        char last = own.charAt(own.length() - 2);
        String tampered = own.substring(0, own.length() - 2) + (last == 'A' ? 'B' : 'A') + own.charAt(own.length() - 1);
        assertThrows(IllegalStateException.class, () -> FieldEncryptor.decrypt(tampered));
    }

    @Test
    void khoáThiếuHoặcSaiĐộDàiThìKhôngKhởiĐộng() {
        assertThrows(IllegalStateException.class, () -> FieldEncryptor.init(""));
        assertThrows(IllegalStateException.class, () -> FieldEncryptor.init("khong-phai-base64!!"));
        assertThrows(IllegalStateException.class, () -> FieldEncryptor.init(Base64.getEncoder().encodeToString(new byte[16])));
    }
}

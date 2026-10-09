package org.example.eventplatform.identity.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Gắn vào trường nhạy cảm bằng {@code @Convert}: ghi xuống CSDL là bản mã, đọc lên là chữ thường. */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return FieldEncryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return FieldEncryptor.decrypt(dbData);
    }
}

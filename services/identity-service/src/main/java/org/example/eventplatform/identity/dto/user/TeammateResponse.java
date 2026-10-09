package org.example.eventplatform.identity.dto.user;

/** Thông tin tối thiểu của đồng đội trong đoàn: đủ để chọn người, không lộ liên hệ hay thông tin tài chính. */
public record TeammateResponse(Long id, String fullName, String roleName) {
}

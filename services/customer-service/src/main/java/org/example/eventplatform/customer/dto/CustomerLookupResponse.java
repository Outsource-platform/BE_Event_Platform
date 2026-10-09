package org.example.eventplatform.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Kết quả tra khách theo số điện thoại cho thành viên: đủ để chọn đúng khách khi tạo show hộ,
 * không lộ số điện thoại, email, địa chỉ hay ghi chú của khách người khác.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLookupResponse {
    private Long id;
    private String fullName;
    private Long assignedToUserId;
    private String assignedToName;
    private boolean mine;
}

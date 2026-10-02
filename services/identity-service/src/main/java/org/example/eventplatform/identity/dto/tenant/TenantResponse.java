package org.example.eventplatform.identity.dto.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.eventplatform.identity.entity.RegistrationStatus;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantResponse {
    private Long id;
    private String name;
    private String domain;
    private String email;
    private boolean active;
    private Boolean isVerified;
    private RegistrationStatus statusConfirm;
    private String category;
    private String province;
    private String ward;
    private String primaryColorHex;
    private String accentColorHex;
}

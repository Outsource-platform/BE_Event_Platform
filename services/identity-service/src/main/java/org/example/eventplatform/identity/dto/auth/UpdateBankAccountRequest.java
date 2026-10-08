package org.example.eventplatform.identity.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBankAccountRequest {
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountHolder;
}

package org.example.eventplatform.identity.dto.tenant;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTenantActiveRequest {

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean active;
}

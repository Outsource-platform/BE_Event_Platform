package org.example.eventplatform.event.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CrewRoleRequest {

    @NotBlank(message = "Bộ phận không được để trống")
    private String department;

    @NotBlank(message = "Tên vị trí không được để trống")
    private String name;

    private String description;
    @jakarta.validation.constraints.DecimalMin(value = "0.0", message = "Cát-xê không được âm")
    private java.math.BigDecimal castFee;
}

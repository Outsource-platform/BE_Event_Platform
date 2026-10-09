package org.example.eventplatform.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrewRoleResponse {
    private Long id;
    private String department;
    private String name;
    private String description;
    private java.math.BigDecimal castFee;
}

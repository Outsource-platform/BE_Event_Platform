package org.example.eventplatform.customer.dto;

import lombok.Getter;
import lombok.Setter;

/** Đổi người phụ trách khách. Để trống (chỉ trưởng đoàn) là bỏ gán: khách của đoàn, không ai hưởng hoa hồng. */
@Getter
@Setter
public class AssigneeRequest {
    private Long assignedToUserId;
}

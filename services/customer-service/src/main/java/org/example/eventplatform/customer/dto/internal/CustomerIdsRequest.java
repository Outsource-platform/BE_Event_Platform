package org.example.eventplatform.customer.dto.internal;

import java.util.List;

/** Danh sách id khách cần tra một lần (service-to-service). */
public record CustomerIdsRequest(List<Long> ids) {
}

package org.example.eventplatform.event.dto;

import java.util.List;

public record PublicShowPage(List<PublicShowResponse> items, int page, int size, long totalElements, int totalPages) {
}

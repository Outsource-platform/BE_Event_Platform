package org.example.eventplatform.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerNoteResponse {
    private Long id;
    private String body;
    private String authorName;
    private LocalDateTime createdAt;
}

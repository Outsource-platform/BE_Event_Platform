package org.example.eventplatform.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerNoteRequest {

    @NotBlank(message = "Nhập nội dung ghi chú")
    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    private String body;
}

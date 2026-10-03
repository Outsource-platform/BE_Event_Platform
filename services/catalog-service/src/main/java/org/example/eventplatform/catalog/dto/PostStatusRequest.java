package org.example.eventplatform.catalog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.example.eventplatform.catalog.entity.PostStatus;

@Getter
@Setter
public class PostStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private PostStatus status;
}

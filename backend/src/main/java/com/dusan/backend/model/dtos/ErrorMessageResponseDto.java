package com.dusan.backend.model.dtos;

import lombok.Data;
import com.dusan.backend.model.ErrorMessage;

import java.time.LocalDateTime;

@Data
public class ErrorMessageResponseDto {
    private Long id;
    private Long userId;
    private String username;
    private Long deliveryId;
    private String statusAtDecline;
    private String message;
    private LocalDateTime createdAt;

    public ErrorMessageResponseDto(ErrorMessage error) {
        this.id = error.getId();
        this.userId = error.getUser().getUserId();
        this.username = error.getUser().getUsername();
        this.deliveryId = error.getDelivery() != null ? error.getDelivery().getId() : null;
        this.statusAtDecline = error.getStatusAtDecline() != null ? error.getStatusAtDecline().name() : null;
        this.message = error.getMessage();
        this.createdAt = error.getCreatedAt();
    }
}

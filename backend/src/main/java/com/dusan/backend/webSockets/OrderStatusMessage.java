package com.dusan.backend.webSockets;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.dusan.backend.enums.DeliveryStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class OrderStatusMessage {
    private Long deliveryId;
    private DeliveryStatus status;
    private String username;
    private LocalDateTime timestamp;
}

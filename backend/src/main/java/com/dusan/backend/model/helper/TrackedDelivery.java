package com.dusan.backend.model.helper;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.dusan.backend.enums.DeliveryStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TrackedDelivery {
    private Long id;
    private DeliveryStatus status;
    private LocalDateTime expectedNextChange;
}

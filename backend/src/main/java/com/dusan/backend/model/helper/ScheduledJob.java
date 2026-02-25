package com.dusan.backend.model.helper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import com.dusan.backend.model.dtos.DeliveryDto;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Getter
public class ScheduledJob {
    private final int jobId;
    private final LocalDateTime scheduledAt;
    private final LocalDateTime estimatedPreparingTime;
    private final int estimatedFirstDelay;
    //    private final DeliveryDto deliveryDto;
//    private final String username;
    private final long deliveryId;
}

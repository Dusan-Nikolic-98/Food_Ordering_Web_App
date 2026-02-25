package com.dusan.backend.model.dtos;

import lombok.Getter;
import lombok.Setter;
import com.dusan.backend.enums.DeliveryStatus;
import com.dusan.backend.model.Delivery;
import com.dusan.backend.model.Dish;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class DeliveryResponseDto {
    private Long id;
    private DeliveryStatus status;
    private String createdByUsername;
    private Boolean active;
    private List<Dish> items;
    private LocalDateTime createdAt;

    public DeliveryResponseDto(Delivery delivery){
        this.id = delivery.getId();
        this.status = delivery.getStatus();
        this.createdByUsername = delivery.getCreatedBy().getUsername();
        this.active = delivery.getActive();
        this.items = delivery.getItems();
        this.createdAt = delivery.getCreatedAt();
    }

}

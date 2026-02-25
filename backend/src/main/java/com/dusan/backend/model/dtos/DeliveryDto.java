package com.dusan.backend.model.dtos;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DeliveryDto {
    @NotEmpty
    private List<String> dishes;//dakle tipa pica,2,732 kao jedan entry

    private LocalDateTime scheduledAt; //bice null za trenutne
}


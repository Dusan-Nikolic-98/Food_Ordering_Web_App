package com.dusan.backend.model;

import lombok.Getter;
import lombok.Setter;
import com.dusan.backend.enums.DeliveryStatus;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class ErrorMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "delivery_id", nullable = true)
    private Delivery delivery;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus statusAtDecline;

    private String message;
}

package com.dusan.backend.model;

import lombok.Getter;
import lombok.Setter;
import com.dusan.backend.enums.DeliveryStatus;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Getter
@Setter
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private Boolean active;

    @Column(length = 2000)
    private String dishesString;

    @Transient
    private List<Dish> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public List<Dish> getItems() {
        if(dishesString != null && !dishesString.isEmpty()) {
            return Stream.of(dishesString.split(";"))
                    .map(s -> {
                        String[] parts = s.split(",");
                        if(parts.length == 3){
                            return new Dish(parts[0],Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                        }
                        return null;
                    }).filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public void setItems(List<Dish> items){
        if(items != null && !items.isEmpty()){
            this.dishesString = items.stream()
                    .map(dish -> dish.getName() + "," + dish.getNoOf() + "," + dish.getPricePerDish())
                    .collect(Collectors.joining(";"));

        }else{
            this.dishesString = "";
        }
        this.items = items;
    }

}


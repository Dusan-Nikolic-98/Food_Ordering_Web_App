package com.dusan.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DishWithImg {
    private String name;
    private Integer pricePerDish;
    private String imageUrl;
}


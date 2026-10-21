package com.example.campusease.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemRequest {
    @NotNull
    private Long foodId;
    @NotNull
    private Integer quantity;
}

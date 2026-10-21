package com.example.campusease.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemUpdateRequest {
    @NotNull
    private Long cartItemId;
    @NotNull
    private Integer quantity;
}

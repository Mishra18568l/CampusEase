package com.example.campusease.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FoodRequest {
    @NotBlank
    private String foodName;
    private String description;
    @NotNull
    private Double price;
    private String imageUrl;
    private Boolean availability;
    private Long categoryId;
    private Integer preparationTime;
    private Boolean vegetarian;
    private Long shopId;
}

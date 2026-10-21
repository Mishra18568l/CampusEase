package com.example.campusease.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShopRequest {
    @NotBlank
    private String shopName;
    private String description;
}

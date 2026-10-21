package com.example.campusease.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PendingReviewResponse {
    private Long orderId;
    private Long foodId;
    private String foodName;
    private String imageUrl;
    private String shopName;
}

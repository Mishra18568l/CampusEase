package com.example.campusease.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckoutResponse {
    private Long orderId;
    private String razorpayOrderId;
    private Double amount;      // in rupees
    private String currency;
    private String razorpayKeyId; // public key id, safe to expose to frontend
}

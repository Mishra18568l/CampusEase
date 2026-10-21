package com.example.campusease.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private String username;
    private Long foodId;
    private String foodName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}

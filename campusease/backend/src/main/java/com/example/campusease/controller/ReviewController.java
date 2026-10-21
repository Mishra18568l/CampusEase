package com.example.campusease.controller;

import com.example.campusease.dto.PendingReviewResponse;
import com.example.campusease.dto.ReviewRequest;
import com.example.campusease.dto.ReviewResponse;
import com.example.campusease.security.CustomUserDetails;
import com.example.campusease.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> submitReview(@AuthenticationPrincipal CustomUserDetails principal,
                                                        @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.submitReview(principal.getUser(), request));
    }

    @GetMapping("/food/{foodId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsForFood(@PathVariable Long foodId) {
        return ResponseEntity.ok(reviewService.getReviewsForFood(foodId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PendingReviewResponse>> getPendingReviews(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(reviewService.getPendingReviews(principal.getUser()));
    }
}

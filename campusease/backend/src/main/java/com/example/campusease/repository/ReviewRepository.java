package com.example.campusease.repository;

import com.example.campusease.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByFoodIdOrderByCreatedAtDesc(Long foodId);
    Optional<Review> findByUserIdAndFoodIdAndOrderId(Long userId, Long foodId, Long orderId);
    boolean existsByUserIdAndFoodIdAndOrderId(Long userId, Long foodId, Long orderId);
}
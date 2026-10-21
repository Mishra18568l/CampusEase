package com.example.campusease.service;

import com.example.campusease.dto.PendingReviewResponse;
import com.example.campusease.dto.ReviewRequest;
import com.example.campusease.dto.ReviewResponse;
import com.example.campusease.exception.BadRequestException;
import com.example.campusease.exception.ResourceNotFoundException;
import com.example.campusease.model.*;
import com.example.campusease.repository.FoodRepository;
import com.example.campusease.repository.OrderRepository;
import com.example.campusease.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final FoodRepository foodRepository;

    public ReviewService(ReviewRepository reviewRepository, OrderRepository orderRepository,
                          FoodRepository foodRepository) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.foodRepository = foodRepository;
    }

    @Transactional
    public ReviewResponse submitReview(User user, ReviewRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("This order does not belong to you.");
        }
        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("You can only review items from orders that have been delivered.");
        }

        boolean orderContainsFood = order.getItems().stream()
                .anyMatch(oi -> oi.getFood().getId().equals(request.getFoodId()));
        if (!orderContainsFood) {
            throw new BadRequestException("This food item wasn't part of that order.");
        }

        if (reviewRepository.existsByUserIdAndFoodIdAndOrderId(user.getId(), request.getFoodId(), order.getId())) {
            throw new BadRequestException("You've already reviewed this item for this order.");
        }

        Food food = foodRepository.findById(request.getFoodId())
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found: " + request.getFoodId()));

        Review review = new Review();
        review.setUser(user);
        review.setFood(food);
        review.setOrder(order);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        reviewRepository.save(review);

        recalculateFoodRating(food);

        return new ReviewResponse(review.getId(), user.getUsername(), food.getId(), food.getFoodName(),
                review.getRating(), review.getComment(), review.getCreatedAt());
    }

    private void recalculateFoodRating(Food food) {
        List<Review> reviews = reviewRepository.findByFoodIdOrderByCreatedAtDesc(food.getId());
        double average = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        food.setRating(Math.round(average * 10.0) / 10.0);
        foodRepository.save(food);
    }

    public List<ReviewResponse> getReviewsForFood(Long foodId) {
        return reviewRepository.findByFoodIdOrderByCreatedAtDesc(foodId).stream()
                .map(r -> new ReviewResponse(r.getId(), r.getUser().getUsername(), r.getFood().getId(),
                        r.getFood().getFoodName(), r.getRating(), r.getComment(), r.getCreatedAt()))
                .toList();
    }

    public List<PendingReviewResponse> getPendingReviews(User user) {
        List<Order> delivered = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.DELIVERED)
                .toList();

        List<PendingReviewResponse> pending = new ArrayList<>();
        for (Order order : delivered) {
            for (OrderItem item : order.getItems()) {
                boolean alreadyReviewed = reviewRepository.existsByUserIdAndFoodIdAndOrderId(
                        user.getId(), item.getFood().getId(), order.getId());
                if (!alreadyReviewed) {
                    Food food = item.getFood();
                    pending.add(new PendingReviewResponse(
                            order.getId(), food.getId(), food.getFoodName(), food.getImageUrl(),
                            food.getShop() != null ? food.getShop().getShopName() : null));
                }
            }
        }
        return pending;
    }
}
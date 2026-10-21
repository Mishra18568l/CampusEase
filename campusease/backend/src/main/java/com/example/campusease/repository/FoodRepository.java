package com.example.campusease.repository;

import com.example.campusease.model.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Long> {
    List<Food> findByFoodNameContainingIgnoreCase(String name);
    List<Food> findByCategoryId(Long categoryId);
    List<Food> findByPriceBetween(Double min, Double max);
    List<Food> findByVegetarian(Boolean vegetarian);
    List<Food> findByAvailability(Boolean availability);
    List<Food> findByShopId(Long shopId);
}

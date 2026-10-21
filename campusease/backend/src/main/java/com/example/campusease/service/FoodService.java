package com.example.campusease.service;

import com.example.campusease.dto.FoodRequest;
import com.example.campusease.exception.ResourceNotFoundException;
import com.example.campusease.model.Category;
import com.example.campusease.model.Food;
import com.example.campusease.repository.FoodRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FoodService {

    private final FoodRepository foodRepository;
    private final CategoryService categoryService;
    private final com.example.campusease.repository.ShopRepository shopRepository;

    public FoodService(FoodRepository foodRepository, CategoryService categoryService,
                        com.example.campusease.repository.ShopRepository shopRepository) {
        this.foodRepository = foodRepository;
        this.categoryService = categoryService;
        this.shopRepository = shopRepository;
    }

    public List<Food> getAll() {
        return foodRepository.findAll();
    }

    public Food getById(Long id) {
        return foodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found: " + id));
    }

    public List<Food> search(String name) {
        return foodRepository.findByFoodNameContainingIgnoreCase(name);
    }

    public List<Food> getByCategory(Long categoryId) {
        return foodRepository.findByCategoryId(categoryId);
    }

    public List<Food> getByPriceRange(Double min, Double max) {
        return foodRepository.findByPriceBetween(min, max);
    }

    public List<Food> filter(Boolean vegetarian, Boolean available) {
        List<Food> result = foodRepository.findAll();
        if (vegetarian != null) {
            result = result.stream().filter(f -> vegetarian.equals(f.getVegetarian())).toList();
        }
        if (available != null) {
            result = result.stream().filter(f -> available.equals(f.getAvailability())).toList();
        }
        return result;
    }

    public Food create(FoodRequest request) {
        Food food = new Food();
        applyRequest(food, request);
        return foodRepository.save(food);
    }

    public Food update(Long id, FoodRequest request) {
        Food food = getById(id);
        applyRequest(food, request);
        return foodRepository.save(food);
    }

    public void delete(Long id) {
        foodRepository.delete(getById(id));
    }

    private void applyRequest(Food food, FoodRequest request) {
        food.setFoodName(request.getFoodName());
        food.setDescription(request.getDescription());
        food.setPrice(request.getPrice());
        food.setImageUrl(request.getImageUrl());
        if (request.getAvailability() != null) {
            food.setAvailability(request.getAvailability());
        }
        if (request.getPreparationTime() != null) {
            food.setPreparationTime(request.getPreparationTime());
        }
        if (request.getVegetarian() != null) {
            food.setVegetarian(request.getVegetarian());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryService.getById(request.getCategoryId());
            food.setCategory(category);
        }
        if (request.getShopId() != null) {
            com.example.campusease.model.Shop shop = shopRepository.findById(request.getShopId())
                    .orElseThrow(() -> new com.example.campusease.exception.ResourceNotFoundException(
                            "Shop not found: " + request.getShopId()));
            food.setShop(shop);
        }
    }

    public List<Food> getByShop(Long shopId) {
        return foodRepository.findByShopId(shopId);
    }

    public Food setAvailability(Long foodId, boolean available) {
        Food food = getById(foodId);
        food.setAvailability(available);
        return foodRepository.save(food);
    }
}
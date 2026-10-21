package com.example.campusease.controller;

import com.example.campusease.dto.FoodRequest;
import com.example.campusease.model.Food;
import com.example.campusease.service.FoodService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @GetMapping
    public ResponseEntity<List<Food>> getAll() {
        return ResponseEntity.ok(foodService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Food> getById(@PathVariable Long id) {
        return ResponseEntity.ok(foodService.getById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Food>> search(@RequestParam String name) {
        return ResponseEntity.ok(foodService.search(name));
    }

    @GetMapping("/category/{id}")
    public ResponseEntity<List<Food>> byCategory(@PathVariable Long id) {
        return ResponseEntity.ok(foodService.getByCategory(id));
    }

    @GetMapping("/price-range")
    public ResponseEntity<List<Food>> byPriceRange(@RequestParam Double min, @RequestParam Double max) {
        return ResponseEntity.ok(foodService.getByPriceRange(min, max));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Food>> filter(@RequestParam(required = false) Boolean vegetarian,
                                              @RequestParam(required = false) Boolean available) {
        return ResponseEntity.ok(foodService.filter(vegetarian, available));
    }

    @PostMapping
    public ResponseEntity<Food> create(@Valid @RequestBody FoodRequest request) {
        return ResponseEntity.ok(foodService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Food> update(@PathVariable Long id, @Valid @RequestBody FoodRequest request) {
        return ResponseEntity.ok(foodService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        foodService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.example.campusease.controller;

import com.example.campusease.dto.ShopRequest;
import com.example.campusease.model.Shop;
import com.example.campusease.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shops")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping
    public ResponseEntity<List<Shop>> getAll() {
        return ResponseEntity.ok(shopService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shop> getById(@PathVariable Long id) {
        return ResponseEntity.ok(shopService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Shop> update(@PathVariable Long id, @Valid @RequestBody ShopRequest request) {
        return ResponseEntity.ok(shopService.update(id, request));
    }

    @PutMapping("/{id}/active")
    public ResponseEntity<Void> setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        shopService.setActive(id, Boolean.TRUE.equals(body.get("active")));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/distribute-foods")
    public ResponseEntity<Map<String, Object>> distributeFoods() {
        int count = shopService.distributeUnassignedFoods();
        return ResponseEntity.ok(Map.of("assignedCount", count));
    }
}
package com.example.campusease.controller;

import com.example.campusease.exception.BadRequestException;
import com.example.campusease.model.Food;
import com.example.campusease.model.Order;
import com.example.campusease.model.OrderStatus;
import com.example.campusease.model.Shop;
import com.example.campusease.security.CustomUserDetails;
import com.example.campusease.service.FoodService;
import com.example.campusease.service.OrderService;
import com.example.campusease.service.ShopService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendor")
public class VendorController {

    private final ShopService shopService;
    private final OrderService orderService;
    private final FoodService foodService;

    public VendorController(ShopService shopService, OrderService orderService, FoodService foodService) {
        this.shopService = shopService;
        this.orderService = orderService;
        this.foodService = foodService;
    }

    @GetMapping("/my-shop")
    public ResponseEntity<Shop> myShop(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(shopService.getByOwner(principal.getUser()));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> myOrders(@AuthenticationPrincipal CustomUserDetails principal) {
        Shop shop = shopService.getByOwner(principal.getUser());
        return ResponseEntity.ok(orderService.getOrdersForShop(shop.getId()));
    }

    @GetMapping("/foods")
    public ResponseEntity<List<Food>> myFoods(@AuthenticationPrincipal CustomUserDetails principal) {
        Shop shop = shopService.getByOwner(principal.getUser());
        return ResponseEntity.ok(foodService.getByShop(shop.getId()));
    }

    @PutMapping("/foods/{id}/availability")
    public ResponseEntity<Food> setFoodAvailability(@AuthenticationPrincipal CustomUserDetails principal,
                                                      @PathVariable Long id,
                                                      @RequestBody Map<String, Boolean> body) {
        Shop shop = shopService.getByOwner(principal.getUser());
        Food food = foodService.getById(id);
        assertOwnsFood(shop, food);
        Boolean available = body.get("available");
        if (available == null) {
            throw new BadRequestException("Missing 'available' field in request body");
        }
        return ResponseEntity.ok(foodService.setAvailability(id, available));
    }

    @PutMapping("/orders/{id}/accept")
    public ResponseEntity<Order> accept(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        assertOwnsOrder(principal, id);
        return ResponseEntity.ok(orderService.updateStatus(id, OrderStatus.ACCEPTED));
    }

    @PutMapping("/orders/{id}/prepare")
    public ResponseEntity<Order> prepare(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        assertOwnsOrder(principal, id);
        return ResponseEntity.ok(orderService.updateStatus(id, OrderStatus.PREPARING));
    }

    @PutMapping("/orders/{id}/ready")
    public ResponseEntity<Order> ready(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        assertOwnsOrder(principal, id);
        return ResponseEntity.ok(orderService.updateStatus(id, OrderStatus.READY));
    }

    @PutMapping("/orders/{id}/deliver")
    public ResponseEntity<Order> deliver(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        assertOwnsOrder(principal, id);
        return ResponseEntity.ok(orderService.updateStatus(id, OrderStatus.DELIVERED));
    }

    @PutMapping("/orders/{id}/reject")
    public ResponseEntity<Order> reject(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        assertOwnsOrder(principal, id);
        return ResponseEntity.ok(orderService.updateStatus(id, OrderStatus.REJECTED));
    }

    @PutMapping("/orders/{id}/out-of-stock")
    public ResponseEntity<Order> outOfStock(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        assertOwnsOrder(principal, id);
        return ResponseEntity.ok(orderService.markOutOfStockAndRefund(id));
    }

    private void assertOwnsOrder(CustomUserDetails principal, Long orderId) {
        Shop shop = shopService.getByOwner(principal.getUser());
        Order order = orderService.getById(orderId);
        if (!order.getShop().getId().equals(shop.getId())) {
            throw new BadRequestException("This order does not belong to your shop.");
        }
    }

    private void assertOwnsFood(Shop shop, Food food) {
        if (food.getShop() == null || !food.getShop().getId().equals(shop.getId())) {
            throw new BadRequestException("This food item does not belong to your shop.");
        }
    }
}
package com.example.campusease.controller;

import com.example.campusease.dto.ShopRequest;
import com.example.campusease.dto.VendorCreateRequest;
import com.example.campusease.model.Order;
import com.example.campusease.model.OrderStatus;
import com.example.campusease.model.PaymentStatus;
import com.example.campusease.model.Shop;
import com.example.campusease.model.User;
import com.example.campusease.service.OrderService;
import com.example.campusease.service.ShopService;
import com.example.campusease.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OrderService orderService;
    private final UserService userService;
    private final ShopService shopService;

    public AdminController(OrderService orderService, UserService userService, ShopService shopService) {
        this.orderService = orderService;
        this.userService = userService;
        this.shopService = shopService;
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/orders/{id}/accept")
    public ResponseEntity<Order> accept(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.updateStatus(id, OrderStatus.ACCEPTED));
    }

    @PutMapping("/orders/{id}/prepare")
    public ResponseEntity<Order> prepare(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.updateStatus(id, OrderStatus.PREPARING));
    }

    @PutMapping("/orders/{id}/ready")
    public ResponseEntity<Order> ready(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.updateStatus(id, OrderStatus.READY));
    }

    @PutMapping("/orders/{id}/deliver")
    public ResponseEntity<Order> deliver(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.updateStatus(id, OrderStatus.DELIVERED));
    }

    @PutMapping("/orders/{id}/reject")
    public ResponseEntity<Order> reject(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.updateStatus(id, OrderStatus.REJECTED));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        List<Order> all = orderService.getAllOrders();
        long pending = all.stream().filter(o -> o.getOrderStatus() == OrderStatus.PENDING).count();
        long preparing = all.stream().filter(o -> o.getOrderStatus() == OrderStatus.PREPARING).count();
        long delivered = all.stream().filter(o -> o.getOrderStatus() == OrderStatus.DELIVERED).count();
        return ResponseEntity.ok(Map.of(
                "totalOrders", all.size(),
                "pendingOrders", pending,
                "preparingOrders", preparing,
                "deliveredOrders", delivered,
                "totalRevenue", orderService.getTotalRevenue()
        ));
    }

    @PostMapping("/vendors")
    public ResponseEntity<Shop> createVendor(@Valid @RequestBody VendorCreateRequest request) {
        User vendor = userService.createVendor(
                request.getUsername(), request.getEmail(), request.getPassword(), request.getPhone());

        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setShopName(request.getShopName());
        shopRequest.setDescription(request.getShopDescription());

        Shop shop = shopService.createWithOwner(shopRequest, vendor);
        return ResponseEntity.ok(shop);
    }

    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> transactions() {
        List<Order> all = orderService.getAllOrders();

        double totalCollected = all.stream()
                .filter(o -> o.getPaymentStatus() == PaymentStatus.PAID)
                .mapToDouble(Order::getTotalAmount).sum();
        double totalRefunded = all.stream()
                .filter(o -> o.getPaymentStatus() == PaymentStatus.REFUNDED)
                .mapToDouble(Order::getTotalAmount).sum();

        Map<String, Double> perShopTotals = new HashMap<>();
        for (Order o : all) {
            if (o.getShop() == null) continue;
            String shopName = o.getShop().getShopName();
            double amount = o.getPaymentStatus() == PaymentStatus.PAID ? o.getTotalAmount() : 0.0;
            perShopTotals.merge(shopName, amount, Double::sum);
        }

        return ResponseEntity.ok(Map.of(
                "orders", all,
                "totalCollected", totalCollected,
                "totalRefunded", totalRefunded,
                "netRevenue", totalCollected - totalRefunded,
                "perShopTotals", perShopTotals
        ));
    }
}
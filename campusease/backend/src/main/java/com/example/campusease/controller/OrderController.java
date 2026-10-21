package com.example.campusease.controller;

import com.example.campusease.dto.CheckoutResponse;
import com.example.campusease.dto.PaymentVerifyRequest;
import com.example.campusease.model.Order;
import com.example.campusease.security.CustomUserDetails;
import com.example.campusease.service.OrderService;
import com.example.campusease.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public OrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    // Creates the order from the cart + a Razorpay order. Call this when the student clicks "Checkout".
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(orderService.checkout(principal.getUser()));
    }

    // Call this from the Razorpay checkout.js success handler to verify & finalize payment.
    @PostMapping("/verify")
    public ResponseEntity<Order> verify(@Valid @RequestBody PaymentVerifyRequest request) {
        return ResponseEntity.ok(paymentService.verifyAndComplete(request));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getCurrentOrders(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(orderService.getCurrentOrders(principal.getUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @GetMapping("/history")
    public ResponseEntity<List<Order>> getHistory(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(orderService.getHistory(principal.getUser()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Order> cancel(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(principal.getUser(), id));
    }
}

package com.example.campusease.controller;

import com.example.campusease.dto.CartItemRequest;
import com.example.campusease.dto.CartItemUpdateRequest;
import com.example.campusease.model.Cart;
import com.example.campusease.security.CustomUserDetails;
import com.example.campusease.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<Cart> getCart(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(cartService.getOrCreateCart(principal.getUser()));
    }

    @PostMapping("/add")
    public ResponseEntity<Cart> add(@AuthenticationPrincipal CustomUserDetails principal,
                                     @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addToCart(principal.getUser(), request));
    }

    @PutMapping("/update")
    public ResponseEntity<Cart> update(@AuthenticationPrincipal CustomUserDetails principal,
                                        @Valid @RequestBody CartItemUpdateRequest request) {
        return ResponseEntity.ok(cartService.updateItem(principal.getUser(), request));
    }

    @DeleteMapping("/remove/{id}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        cartService.removeItem(principal.getUser(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clear(@AuthenticationPrincipal CustomUserDetails principal) {
        cartService.clearCart(principal.getUser());
        return ResponseEntity.noContent().build();
    }
}

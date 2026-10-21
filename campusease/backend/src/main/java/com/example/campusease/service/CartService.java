package com.example.campusease.service;

import com.example.campusease.dto.CartItemRequest;
import com.example.campusease.dto.CartItemUpdateRequest;
import com.example.campusease.exception.BadRequestException;
import com.example.campusease.exception.ResourceNotFoundException;
import com.example.campusease.model.*;
import com.example.campusease.repository.CartItemRepository;
import com.example.campusease.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FoodService foodService;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, FoodService foodService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.foodService = foodService;
    }

    @Transactional
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    @Transactional
    public Cart addToCart(User user, CartItemRequest request) {
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        Food food = foodService.getById(request.getFoodId());
if (Boolean.FALSE.equals(food.getAvailability())) {
    throw new BadRequestException("This item is currently unavailable");
}
if (food.getShop() == null) {
    throw new BadRequestException("This item isn't assigned to a shop yet and can't be ordered.");
}
Cart cart = getOrCreateCart(user);

boolean cartHasOtherShopItems = cart.getItems().stream()
        .anyMatch(i -> !i.getFood().getShop().getId().equals(food.getShop().getId()));
if (cartHasOtherShopItems) {
    throw new BadRequestException(
        "Your cart has items from another shop (" + cart.getItems().get(0).getFood().getShop().getShopName()
        + "). Clear your cart first to order from " + food.getShop().getShopName() + ".");
}

CartItem existing = cartItemRepository.findByCartIdAndFoodId(cart.getId(), food.getId()).orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            existing.setSubtotal(existing.getQuantity() * food.getPrice());
            cartItemRepository.save(existing);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setFood(food);
            item.setQuantity(request.getQuantity());
            item.setSubtotal(request.getQuantity() * food.getPrice());
            cart.getItems().add(item);
            cartItemRepository.save(item);
        }
        return cartRepository.findById(cart.getId()).orElseThrow();
    }

    @Transactional
    public Cart updateItem(User user, CartItemUpdateRequest request) {
        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findById(request.getCartItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("This item does not belong to your cart");
        }
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        item.setQuantity(request.getQuantity());
        item.setSubtotal(request.getQuantity() * item.getFood().getPrice());
        cartItemRepository.save(item);
        return cartRepository.findById(cart.getId()).orElseThrow();
    }

    @Transactional
public void removeItem(User user, Long cartItemId) {
    Cart cart = getOrCreateCart(user);
    CartItem item = cart.getItems().stream()
            .filter(i -> i.getId().equals(cartItemId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
    cart.getItems().remove(item);
    cartRepository.save(cart);
}

    @Transactional
    public void clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    public double calculateTotal(Cart cart) {
        return cart.getItems().stream().mapToDouble(CartItem::getSubtotal).sum();
    }
}

package com.example.campusease.service;

import com.example.campusease.dto.CheckoutResponse;
import com.example.campusease.exception.BadRequestException;
import com.example.campusease.exception.ResourceNotFoundException;
import com.example.campusease.model.*;
import com.example.campusease.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final PaymentService paymentService;
    private final FoodService foodService;

    private static final List<OrderStatus> INACTIVE_STATUSES =
            List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.REJECTED);

    public OrderService(OrderRepository orderRepository, CartService cartService, PaymentService paymentService,
                         FoodService foodService) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.paymentService = paymentService;
        this.foodService = foodService;
    }

    public List<Order> getOrdersForShop(Long shopId) {
        return orderRepository.findByShopIdOrderByCreatedAtDesc(shopId);
    }

    @Transactional
    public Order markOutOfStockAndRefund(Long orderId) {
        Order order = getById(orderId);
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BadRequestException("Only paid orders can be refunded through this action.");
        }
        paymentService.refundOrder(order);
        order.setOrderStatus(OrderStatus.REJECTED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        return orderRepository.save(order);
    }

    @Transactional
    public CheckoutResponse checkout(User user) {
        Cart cart = cartService.getOrCreateCart(user);
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        List<String> nowUnavailable = cart.getItems().stream()
                .filter(ci -> Boolean.FALSE.equals(foodService.getById(ci.getFood().getId()).getAvailability()))
                .map(ci -> ci.getFood().getFoodName())
                .toList();
        if (!nowUnavailable.isEmpty()) {
            throw new BadRequestException(
                "Sorry, this item just went unavailable: " + String.join(", ", nowUnavailable)
                + ". Please remove it from your cart before checking out.");
        }

        double total = cartService.calculateTotal(cart);

        Order order = new Order();
        order.setUser(user);
        order.setShop(cart.getItems().get(0).getFood().getShop());
        order.setTotalAmount(total);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setOrderStatus(OrderStatus.PENDING);

        for (CartItem ci : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setFood(ci.getFood());
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(ci.getFood().getPrice());
            oi.setSubtotal(ci.getSubtotal());
            order.getItems().add(oi);
        }

        Order saved = orderRepository.save(order);
        return paymentService.createRazorpayOrder(saved);
    }

    @Transactional
    public void markPaid(Order order) {
        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);
        cartService.clearCart(order.getUser());
    }

    public Order getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    public List<Order> getCurrentOrders(User user) {
        return orderRepository.findByUserIdAndOrderStatusNotInOrderByCreatedAtDesc(user.getId(), INACTIVE_STATUSES);
    }

    public List<Order> getHistory(User user) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public Order cancelOrder(User user, Long orderId) {
        Order order = getById(orderId);
        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("This order does not belong to you");
        }
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Only pending orders can be cancelled");
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Order updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = getById(orderId);
        order.setOrderStatus(newStatus);
        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveryTime(LocalDateTime.now());
        }
        return orderRepository.save(order);
    }

    public double getTotalRevenue() {
        return orderRepository.findAll().stream()
                .filter(o -> o.getPaymentStatus() == PaymentStatus.PAID)
                .mapToDouble(Order::getTotalAmount)
                .sum();
    }
}
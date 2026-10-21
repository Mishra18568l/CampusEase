package com.example.campusease.repository;

import com.example.campusease.model.Order;
import com.example.campusease.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByUserIdAndOrderStatusNotInOrderByCreatedAtDesc(Long userId, List<OrderStatus> statuses);
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByOrderStatus(OrderStatus status);
    List<Order> findByShopIdOrderByCreatedAtDesc(Long shopId);
}

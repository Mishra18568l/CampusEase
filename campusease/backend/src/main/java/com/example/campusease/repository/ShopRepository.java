package com.example.campusease.repository;

import com.example.campusease.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, Long> {
    Optional<Shop> findByOwnerId(Long ownerId);
    boolean existsByOwnerId(Long ownerId);
}
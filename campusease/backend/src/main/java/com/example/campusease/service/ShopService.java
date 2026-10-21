package com.example.campusease.service;

import com.example.campusease.dto.ShopRequest;
import com.example.campusease.exception.BadRequestException;
import com.example.campusease.exception.ResourceNotFoundException;
import com.example.campusease.model.Food;
import com.example.campusease.model.Shop;
import com.example.campusease.model.User;
import com.example.campusease.repository.FoodRepository;
import com.example.campusease.repository.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
public class ShopService {

    private final ShopRepository shopRepository;
    private final FoodRepository foodRepository;

    public ShopService(ShopRepository shopRepository, FoodRepository foodRepository) {
        this.shopRepository = shopRepository;
        this.foodRepository = foodRepository;
    }

    public List<Shop> getAll() {
        return shopRepository.findAll();
    }

    public Shop getById(Long id) {
        return shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found: " + id));
    }

    public Shop getByOwner(User owner) {
        return shopRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No shop is registered to this vendor account yet."));
    }

    @Transactional
    public Shop createWithOwner(ShopRequest request, User owner) {
        if (shopRepository.existsByOwnerId(owner.getId())) {
            throw new BadRequestException("This vendor already owns a shop");
        }
        Shop shop = new Shop();
        shop.setShopName(request.getShopName());
        shop.setDescription(request.getDescription());
        shop.setOwner(owner);
        shop.setActive(true);
        return shopRepository.save(shop);
    }

    @Transactional
    public Shop update(Long id, ShopRequest request) {
        Shop shop = getById(id);
        shop.setShopName(request.getShopName());
        shop.setDescription(request.getDescription());
        return shopRepository.save(shop);
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        Shop shop = getById(id);
        shop.setActive(active);
        shopRepository.save(shop);
    }

    @Transactional
    public int distributeUnassignedFoods() {
        List<Shop> shops = shopRepository.findAll();
        if (shops.isEmpty()) {
            throw new BadRequestException("Create at least one shop before distributing food items.");
        }
        List<Food> unassigned = foodRepository.findAll().stream()
                .filter(f -> f.getShop() == null)
                .toList();
        Random random = new Random();
        for (Food food : unassigned) {
            Shop randomShop = shops.get(random.nextInt(shops.size()));
            food.setShop(randomShop);
            foodRepository.save(food);
        }
        return unassigned.size();
    }
}
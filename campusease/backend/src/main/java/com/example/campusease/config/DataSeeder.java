package com.example.campusease.config;

import com.example.campusease.model.Category;
import com.example.campusease.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

// Seeds a starter set of categories on first run so the menu isn't empty. Safe to run repeatedly.
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public DataSeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return;
        }
        List<String> defaults = List.of(
                "Fast Food", "South Indian", "North Indian", "Chinese",
                "Beverages", "Snacks", "Desserts"
        );
        for (String name : defaults) {
            Category c = new Category();
            c.setCategoryName(name);
            categoryRepository.save(c);
        }
        System.out.println("Seeded default food categories.");
    }
}

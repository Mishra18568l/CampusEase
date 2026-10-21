package com.example.campusease.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "food_items")
@Data
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "food_name", nullable = false)
    private String foodName;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(name = "image_url")
    private String imageUrl;

    private Boolean availability = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "shop_id")
private Shop shop;

    @Column(name = "preparation_time")
    private Integer preparationTime;

    private Double rating = 0.0;

    private Boolean vegetarian = true;
}

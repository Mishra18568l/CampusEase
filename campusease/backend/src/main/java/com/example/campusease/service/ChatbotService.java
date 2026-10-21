package com.example.campusease.service;

import com.example.campusease.model.Food;
import com.example.campusease.repository.FoodRepository;
import com.example.campusease.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private final FoodRepository foodRepository;
    private final ReviewRepository reviewRepository;

    private static final Set<String> STOPWORDS = Set.of(
            "which", "shop", "shops", "sells", "sell", "sold", "best", "good", "great",
            "the", "is", "a", "an", "for", "of", "in", "on", "at", "where", "can", "i",
            "get", "find", "recommend", "suggest", "food", "item", "items", "to", "that",
            "tastes", "taste", "me", "you", "please", "hi", "hello", "hey", "does", "do",
            "any", "some", "has", "have", "what", "who", "with", "and", "or", "campus"
    );

    public ChatbotService(FoodRepository foodRepository, ReviewRepository reviewRepository) {
        this.foodRepository = foodRepository;
        this.reviewRepository = reviewRepository;
    }

    public String answer(String message) {
        List<String> queryWords = tokenize(message);
        if (queryWords.isEmpty()) {
            return fallback();
        }

        List<Food> allFoods = foodRepository.findAll().stream()
                .filter(f -> f.getShop() != null)
                .toList();
        if (allFoods.isEmpty()) {
            return "The menu doesn't have any items assigned to shops yet -- ask an admin to set that up first.";
        }

        Food bestMatch = null;
        int bestScore = 0;
        for (Food food : allFoods) {
            List<String> foodWords = tokenize(food.getFoodName());
            int score = (int) foodWords.stream().filter(queryWords::contains).count();
            if (score > bestScore) {
                bestScore = score;
                bestMatch = food;
            }
        }

        if (bestMatch == null || bestScore == 0) {
            return fallback();
        }

        String targetName = bestMatch.getFoodName();
        List<Food> sameNameAcrossShops = allFoods.stream()
                .filter(f -> f.getFoodName().equalsIgnoreCase(targetName))
                .sorted(Comparator.comparingDouble(Food::getRating).reversed())
                .toList();

        return buildReply(targetName, sameNameAcrossShops);
    }

    private String buildReply(String foodName, List<Food> options) {
        StringBuilder sb = new StringBuilder();

        if (options.size() == 1) {
            Food f = options.get(0);
            int reviewCount = reviewRepository.findByFoodIdOrderByCreatedAtDesc(f.getId()).size();
            sb.append(String.format("%s is available at %s", foodName, f.getShop().getShopName()));
            if (reviewCount > 0) {
                sb.append(String.format(", rated %.1f\u2605 from %d review%s.",
                        f.getRating(), reviewCount, reviewCount == 1 ? "" : "s"));
            } else {
                sb.append(", but no one has rated it yet -- be the first!");
            }
        } else {
            sb.append(String.format("Here's where you can get %s, sorted by rating:\n", foodName));
            int rank = 1;
            for (Food f : options) {
                int reviewCount = reviewRepository.findByFoodIdOrderByCreatedAtDesc(f.getId()).size();
                sb.append(String.format("%d. %s -- ", rank, f.getShop().getShopName()));
                if (reviewCount > 0) {
                    sb.append(String.format("%.1f\u2605 (%d review%s)", f.getRating(), reviewCount, reviewCount == 1 ? "" : "s"));
                } else {
                    sb.append("no ratings yet");
                }
                sb.append("\n");
                rank++;
            }
        }
        return sb.toString().trim();
    }

    private String fallback() {
        List<Food> topRated = foodRepository.findAll().stream()
                .filter(f -> f.getShop() != null && f.getRating() != null && f.getRating() > 0)
                .sorted(Comparator.comparingDouble(Food::getRating).reversed())
                .limit(3)
                .toList();

        StringBuilder sb = new StringBuilder("I couldn't find that on the menu. ");
        if (topRated.isEmpty()) {
            sb.append("Try asking about a specific dish, like \"which shop has good samosas\".");
        } else {
            sb.append("Some highly-rated items right now: ");
            sb.append(topRated.stream()
                    .map(f -> String.format("%s (%.1f\u2605 at %s)", f.getFoodName(), f.getRating(), f.getShop().getShopName()))
                    .collect(Collectors.joining(", ")));
        }
        return sb.toString();
    }

    private List<String> tokenize(String text) {
        String cleaned = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        return Arrays.stream(cleaned.split("\\s+"))
                .filter(w -> w.length() > 2 && !STOPWORDS.contains(w))
                .toList();
    }
}

package com.example.campusease.service;

import com.example.campusease.dto.CategoryRequest;
import com.example.campusease.exception.ResourceNotFoundException;
import com.example.campusease.model.Category;
import com.example.campusease.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    public Category create(CategoryRequest request) {
        Category category = new Category();
        category.setCategoryName(request.getCategoryName());
        return categoryRepository.save(category);
    }

    public Category update(Long id, CategoryRequest request) {
        Category category = getById(id);
        category.setCategoryName(request.getCategoryName());
        return categoryRepository.save(category);
    }

    public void delete(Long id) {
        categoryRepository.delete(getById(id));
    }
}

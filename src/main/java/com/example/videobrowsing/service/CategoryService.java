package com.example.videobrowsing.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.videobrowsing.entity.Category;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.repository.CategoryRepository;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Get all categories ordered by name
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAllOrderByName();
    }

    /**
     * Get category by ID
     */
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    /**
     * Get category by name
     */
    public Optional<Category> getCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }

    /**
     * Check if category exists by name
     */
    public Boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }

    /**
     * Create new category
     */
    @Transactional
    public Category createCategory(String name, String description, User createdBy) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setCreatedBy(createdBy);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        
        return categoryRepository.save(category);
    }

    /**
     * Update category
     */
    @Transactional
    public Category updateCategory(Long id, String name, String description) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        category.setName(name);
        category.setDescription(description);
        category.setUpdatedAt(LocalDateTime.now());
        
        return categoryRepository.save(category);
    }

    /**
     * Delete category
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        categoryRepository.delete(category);
    }

    /**
     * Search categories by keyword
     */
    public List<Category> searchCategories(String keyword) {
        return categoryRepository.searchCategories(keyword);
    }

    /**
     * Count total categories
     */
    public long countCategories() {
        return categoryRepository.count();
    }
}

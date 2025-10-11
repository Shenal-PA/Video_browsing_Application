package com.example.videobrowsing.service;


import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.videobrowsing.entity.Category;
import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.repository.CategoryRepository;
import com.example.videobrowsing.repository.VideoRepository;

@Service
public class SearchService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Video> searchVideos(String keyword, Long categoryId) {
        if (categoryId != null && categoryId > 0) {
            // Search within specific category
            return videoRepository.findByCategoryIdAndPrivacyOrderByCreatedAtDesc(categoryId, Video.Privacy.PUBLIC);
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            // General search - search only in title
            String trimmed = keyword.trim();
            return videoRepository
                    .findByTitleContainingIgnoreCase(trimmed)
                    .stream()
                    .filter(video -> video.getPrivacy() == Video.Privacy.PUBLIC)
                    .toList();
        } else {
            // Return all public videos if no criteria
            return videoRepository.findByPrivacyOrderByCreatedAtDesc(Video.Privacy.PUBLIC);
        }
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAllOrderByName();
    }

    public List<Category> searchCategories(String keyword) {
        return categoryRepository.searchCategories(keyword);
    }

    public Category createCategory(String name, String description) {
        if (categoryRepository.existsByName(name)) {
            throw new RuntimeException("Category already exists");
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long categoryId, String name, String description) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (name != null && !name.equals(category.getName())) {
            if (categoryRepository.existsByName(name)) {
                throw new RuntimeException("Category name already exists");
            }
            category.setName(name);
        }

        if (description != null) {
            category.setDescription(description);
        }

        return categoryRepository.save(category);
    }

    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        categoryRepository.delete(category);
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public Optional<Category> getCategoryByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return categoryRepository.findByName(name.trim());
    }
}
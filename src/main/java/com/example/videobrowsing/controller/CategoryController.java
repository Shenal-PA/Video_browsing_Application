package com.example.videobrowsing.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.videobrowsing.entity.Category;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.service.CategoryService;
import com.example.videobrowsing.service.UserService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    /**
     * Get all categories
     */
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Get category by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        Optional<Category> category = categoryService.getCategoryById(id);
        if (category.isPresent()) {
            return ResponseEntity.ok(category.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Category not found"));
    }

    /**
    * Create new category (Admin only)
     */
    @PostMapping
    public ResponseEntity<?> createCategory(
            @RequestBody Map<String, String> request,
            HttpSession session) {
        try {
            // Check if user is logged in
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not logged in"));
            }

            // Get user and check if admin
            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();
            if (user.getRole() == null || !user.getRole().name().equals("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin access required"));
            }

            // Validate input
            String name = request.get("name");
            String description = request.get("description");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Category name is required"));
            }

            // Check if category already exists
            if (categoryService.existsByName(name.trim())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Category '" + name.trim() + "' already exists"));
            }

            // Create category
            Category category = categoryService.createCategory(name.trim(), description, user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Category created successfully");
            response.put("category", category);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create category: " + e.getMessage()));
        }
    }

    /**
     * Update category (Admin only)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            HttpSession session) {
        try {
            // Check if user is logged in and is admin
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not logged in"));
            }

            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isEmpty() || userOpt.get().getRole() == null || 
                !userOpt.get().getRole().name().equals("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin access required"));
            }

            // Get category
            Optional<Category> categoryOpt = categoryService.getCategoryById(id);
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Category not found"));
            }

            String name = request.get("name");
            String description = request.get("description");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Category name is required"));
            }

            // Check if new name conflicts with existing category
            Category existingCategory = categoryOpt.get();
            if (!existingCategory.getName().equals(name.trim()) && 
                categoryService.existsByName(name.trim())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Category name '" + name.trim() + "' already exists"));
            }

            // Update category
            Category updatedCategory = categoryService.updateCategory(id, name.trim(), description);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Category updated successfully");
            response.put("category", updatedCategory);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update category: " + e.getMessage()));
        }
    }

    /**
     * Delete category (Admin only)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id, HttpSession session) {
        try {
            // Check if user is logged in and is admin
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not logged in"));
            }

            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isEmpty() || userOpt.get().getRole() == null || 
                !userOpt.get().getRole().name().equals("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin access required"));
            }

            // Check if category exists
            Optional<Category> categoryOpt = categoryService.getCategoryById(id);
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Category not found"));
            }

            // Delete category
            categoryService.deleteCategory(id);

            return ResponseEntity.ok(Map.of("message", "Category deleted successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete category: " + e.getMessage()));
        }
    }

    /**
     * Search categories
     */
    @GetMapping("/search")
    public ResponseEntity<List<Category>> searchCategories(@RequestParam String keyword) {
        List<Category> categories = categoryService.searchCategories(keyword);
        return ResponseEntity.ok(categories);
    }

    
}


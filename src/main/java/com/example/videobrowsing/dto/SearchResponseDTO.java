package com.example.videobrowsing.dto;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.example.videobrowsing.entity.Category;

public class SearchResponseDTO {

    private String message;
    private List<VideoSummaryDTO> videos;
    private List<CategorySummary> categories;

    public SearchResponseDTO() {
    }

    public SearchResponseDTO(String message, List<VideoSummaryDTO> videos) {
        this.message = message;
        this.videos = videos;
        this.categories = Collections.emptyList();
    }

    public static SearchResponseDTO forCategories(List<Category> categories) {
        SearchResponseDTO response = new SearchResponseDTO();
        response.setMessage(categories.isEmpty()
                ? "No categories found."
                : String.format("Found %d categor%s.", categories.size(), categories.size() == 1 ? "y" : "ies"));
        response.setCategories(categories.stream()
                .map(CategorySummary::from)
                .collect(Collectors.toList()));
        response.setVideos(Collections.emptyList());
        return response;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<VideoSummaryDTO> getVideos() {
        return videos;
    }

    public void setVideos(List<VideoSummaryDTO> videos) {
        this.videos = videos;
    }

    public List<CategorySummary> getCategories() {
        return categories;
    }

    public void setCategories(List<CategorySummary> categories) {
        this.categories = categories;
    }

    public static class CategorySummary {
        private Long id;
        private String name;
        private String description;

        public static CategorySummary from(Category category) {
            CategorySummary summary = new CategorySummary();
            summary.setId(category.getId());
            summary.setName(category.getName());
            summary.setDescription(category.getDescription());
            return summary;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}

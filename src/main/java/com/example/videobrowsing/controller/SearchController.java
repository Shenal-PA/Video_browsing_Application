package com.example.videobrowsing.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.videobrowsing.dto.SearchResponseDTO;
import com.example.videobrowsing.dto.VideoSummaryDTO;
import com.example.videobrowsing.entity.Category;
import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.service.SearchService;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {

	@Autowired
	private SearchService searchService;

	@GetMapping("/videos")
	public ResponseEntity<SearchResponseDTO> searchVideos(
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "categoryId", required = false) Long categoryId,
			@RequestParam(name = "category", required = false) String categoryName) {

		Long resolvedCategoryId = resolveCategoryId(categoryId, categoryName);

		if (StringUtils.hasText(categoryName) && resolvedCategoryId == null) {
			String message = String.format("We couldn't find a category named '%s'.", categoryName.trim());
			return ResponseEntity.ok(new SearchResponseDTO(message, List.of()));
		}

		List<Video> videos = searchService.searchVideos(keyword, resolvedCategoryId);
		List<VideoSummaryDTO> results = videos.stream()
				.map(this::toVideoSummary)
				.collect(Collectors.toList());

		String message;
		if (!StringUtils.hasText(keyword) && resolvedCategoryId == null) {
			message = results.isEmpty()
					? "No public videos are available yet."
					: String.format("Showing %d public video%s.", results.size(), results.size() == 1 ? "" : "s");
		} else if (results.isEmpty()) {
			message = "We couldn't find any videos that match your search.";
		} else {
			message = String.format("Found %d video%s.", results.size(), results.size() == 1 ? "" : "s");
		}

		SearchResponseDTO response = new SearchResponseDTO(message, results);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/categories")
	public ResponseEntity<SearchResponseDTO> searchCategories(
			@RequestParam(name = "keyword", required = false, defaultValue = "") String keyword) {

		List<Category> categories = StringUtils.hasText(keyword)
				? searchService.searchCategories(keyword)
				: searchService.getAllCategories();

		SearchResponseDTO response = SearchResponseDTO.forCategories(categories);
		return ResponseEntity.ok(response);
	}

	private Long resolveCategoryId(Long categoryId, String categoryName) {
		if (categoryId != null && categoryId > 0) {
			return categoryId;
		}

		if (StringUtils.hasText(categoryName)) {
			Optional<Category> categoryOpt = searchService.getCategoryByName(categoryName.trim());
			if (categoryOpt.isPresent()) {
				return categoryOpt.get().getId();
			}
		}
		return null;
	}

	private VideoSummaryDTO toVideoSummary(Video video) {
		VideoSummaryDTO summary = new VideoSummaryDTO();
		summary.setId(video.getId());
		summary.setTitle(video.getTitle());
		summary.setDescription(video.getDescription());
	Integer duration = video.getDuration();
	Long viewCount = video.getViewCount();
	Long likeCount = video.getLikeCount();

	summary.setDuration(Optional.ofNullable(duration).orElse(0));
	summary.setViewCount(Optional.ofNullable(viewCount).orElse(0L));
	summary.setLikeCount(Optional.ofNullable(likeCount).orElse(0L));
		summary.setCategoryName(video.getCategory() != null ? video.getCategory().getName() : null);
		summary.setUploaderName(video.getUploadedBy() != null ? video.getUploadedBy().getUsername() : "Unknown");
		summary.setThumbnailUrl(video.getThumbnail() != null
				? "/uploads/thumbnails/" + video.getThumbnail()
				: "/images/default-thumbnail.jpg");
		summary.setCreatedAt(video.getCreatedAt() != null ? video.getCreatedAt().toString() : null);
		return summary;
	}


	
}


package com.example.videobrowsing.service;



import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import com.example.videobrowsing.dto.VideoDTO;
import com.example.videobrowsing.dto.VideoRatingSummary;
import com.example.videobrowsing.entity.Rating;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.entity.VideoUserRating;
import com.example.videobrowsing.repository.CategoryRepository;
import com.example.videobrowsing.repository.RatingRepository;
import com.example.videobrowsing.repository.VideoRepository;
import com.example.videobrowsing.repository.VideoUserRatingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class VideoService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private VideoUserRatingRepository videoUserRatingRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public VideoDTO toDto(Video video, Optional<User> currentUser) {
        if (video == null) {
            return null;
        }

        VideoDTO dto = new VideoDTO();
        dto.setId(video.getId());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());

        String videoUrl = resolveVideoUrl(video);
        dto.setFilePath(videoUrl);
        dto.setVideoUrl(videoUrl);

        String thumbnailUrl = resolveThumbnailUrl(video);
        dto.setThumbnailPath(thumbnailUrl);
        dto.setThumbnailUrl(thumbnailUrl);

        dto.setDuration(video.getDuration());
        dto.setFileSize(video.getFileSize());
        dto.setResolution(video.getResolution());

        if (video.getCategory() != null) {
            dto.setCategoryId(video.getCategory().getId());
            dto.setCategoryName(video.getCategory().getName());
        }

        User uploader = video.getUploadedBy();
        if (uploader != null) {
            dto.setUploadedById(uploader.getId());
            dto.setUploaderName(resolveDisplayName(uploader));
            dto.setUploaderAvatar(uploader.getProfilePicture());
            dto.setUploaderRole(uploader.getRole() != null ? uploader.getRole().name() : null);
            dto.setUploaderJoinDate(uploader.getCreatedAt() != null ? uploader.getCreatedAt().toString() : null);
        }

        long subscriberCount = uploader != null ? subscriptionService.countSubscribers(uploader) : 0L;
        dto.setUploaderSubscriberCount(subscriberCount);

        boolean subscribedToUploader = currentUser
            .filter(user -> uploader != null && !user.getId().equals(uploader.getId()))
            .map(user -> subscriptionService.isSubscribed(user, uploader))
            .orElse(false);
        dto.setSubscribedToUploader(subscribedToUploader);

        dto.setPrivacy(video.getPrivacy() != null ? video.getPrivacy().name() : null);
        dto.setStatus(video.getStatus() != null ? video.getStatus().name() : null);
        dto.setViewCount(Optional.ofNullable(video.getViewCount()).orElse(0L));
        dto.setLikeCount(Optional.ofNullable(video.getLikeCount()).orElse(0L));
        dto.setDislikeCount(Optional.ofNullable(video.getDislikeCount()).orElse(0L));
        dto.setTags(video.getTags());
        dto.setCreatedAt(video.getCreatedAt() != null ? video.getCreatedAt().toString() : null);

        currentUser
            .flatMap(user -> findRatingForUser(video, user))
            .ifPresent(rating -> {
                dto.setLikedByCurrentUser(rating.getRatingType() == Rating.RatingType.LIKE);
                dto.setDislikedByCurrentUser(rating.getRatingType() == Rating.RatingType.DISLIKE);
            });

        VideoRatingSummary ratingSummary;
        try {
            ratingSummary = getVideoRatingSummary(video.getId(), currentUser);
        } catch (Exception ex) {
            ratingSummary = new VideoRatingSummary(0.0, 0L, null);
        }

        dto.setAverageRating(ratingSummary.getAverageRating());
        dto.setRatingCount(ratingSummary.getRatingCount());
        dto.setUserRating(ratingSummary.getUserRating());

        return dto;
    }

    public List<VideoDTO> toDtos(List<Video> videos, Optional<User> currentUser) {
        if (videos == null || videos.isEmpty()) {
            return List.of();
        }
        return videos.stream()
            .map(video -> toDto(video, currentUser))
            .collect(Collectors.toList());
    }

    private String resolveThumbnailUrl(Video video) {
        if (video == null) {
            return "/images/default-thumbnail.jpg";
        }

        String thumbnail = video.getThumbnail();
        if (thumbnail == null || thumbnail.isBlank()) {
            return "/images/default-thumbnail.jpg";
        }

        String normalized = thumbnail.trim().replace("\\", "/");
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }

        String resolved;
        if (normalized.startsWith("/")) {
            resolved = normalized;
        } else if (normalized.startsWith("uploads/") || normalized.startsWith("static/") || normalized.startsWith("thumbnails/") || normalized.startsWith("images/")) {
            resolved = "/" + normalized;
        } else {
            int uploadsIndex = normalized.indexOf("/uploads/");
            if (uploadsIndex >= 0) {
                resolved = normalized.substring(uploadsIndex);
                if (!resolved.startsWith("/")) {
                    resolved = "/" + resolved;
                }
            } else if (normalized.contains("/")) {
                resolved = normalized.startsWith("/") ? normalized : "/" + normalized;
            } else {
                resolved = "/uploads/thumbnails/" + normalized;
            }
        }

        return encodeForUrl(resolved);
    }

    private String resolveVideoUrl(Video video) {
        if (video == null) {
            return null;
        }

        String path = video.getFilepath();
        if (path == null || path.isBlank()) {
            return null;
        }

        String normalized = path.trim().replace("\\", "/");
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }

        String resolved;
        if (normalized.startsWith("/")) {
            resolved = normalized;
        } else if (normalized.startsWith("uploads/") || normalized.startsWith("videos/") || normalized.startsWith("video/") || normalized.startsWith("static/")) {
            resolved = "/" + normalized;
        } else {
            int uploadsIndex = normalized.indexOf("/uploads/");
            if (uploadsIndex >= 0) {
                resolved = normalized.substring(uploadsIndex);
                if (!resolved.startsWith("/")) {
                    resolved = "/" + resolved;
                }
            } else if (normalized.contains("/")) {
                resolved = normalized.startsWith("/") ? normalized : "/" + normalized;
            } else {
                resolved = "/uploads/videos/" + normalized;
            }
        }

        return encodeForUrl(resolved);
    }

    private String resolveDisplayName(User user) {
        if (user == null) {
            return "Unknown Creator";
        }

        String first = user.getFirstname();
        String last = user.getLastname();
        if (first != null && !first.isBlank()) {
            String combined = first + (last != null && !last.isBlank() ? " " + last : "");
            return combined.trim();
        }

        String username = user.getUsername();
        if (username != null && !username.isBlank()) {
            return username;
        }

        return Optional.ofNullable(user.getEmail()).orElse("Creator");
    }

    public Video uploadVideo(VideoDTO videoDTO, MultipartFile videoFile, MultipartFile thumbnailFile, User uploader) throws IOException {
        // Save video file
        String videoFileName = UUID.randomUUID() + "_" + videoFile.getOriginalFilename();
        Path videoPath = Paths.get(uploadDir, "videos", videoFileName);
        Files.createDirectories(videoPath.getParent());
        Files.write(videoPath, videoFile.getBytes());

        // Save thumbnail file if provided
        String thumbnailFileName = null;
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            thumbnailFileName = UUID.randomUUID() + "_" + thumbnailFile.getOriginalFilename();
            Path thumbnailPath = Paths.get(uploadDir, "thumbnails", thumbnailFileName);
            Files.createDirectories(thumbnailPath.getParent());
            Files.write(thumbnailPath, thumbnailFile.getBytes());
        }

        // Create and save video entity
        Video video = new Video();
        video.setTitle(videoDTO.getTitle());
        video.setDescription(videoDTO.getDescription());
        video.setFilepath(videoFileName);
        video.setThumbnail(thumbnailFileName);
        video.setUploadedBy(uploader);
        video.setTags(normalizeTags(videoDTO.getTags()));

        // Convert string privacy to enum
        if (videoDTO.getPrivacy() != null) {
            try {
                video.setPrivacy(Video.Privacy.valueOf(videoDTO.getPrivacy().toUpperCase()));
            } catch (IllegalArgumentException e) {
                video.setPrivacy(Video.Privacy.PUBLIC); // Default to PUBLIC if invalid
            }
        }

        video.setStatus(Video.Status.PUBLISHED);
        video.setViewCount(0L);

        // Set category
        categoryRepository.findById(videoDTO.getCategoryId())
            .ifPresent(video::setCategory);

        return videoRepository.save(video);
    }

    public List<Video> getAllPublicVideos() {
        return videoRepository.findByPrivacyOrderByCreatedAtDesc(Video.Privacy.PUBLIC);
    }

    public List<Video> getMostViewedVideos() {
        return videoRepository.findByPrivacyOrderByViewCountDesc(Video.Privacy.PUBLIC);
    }

    public List<Video> getMostLikedVideos() {
        return videoRepository.findByPrivacyOrderByLikeCountDesc(Video.Privacy.PUBLIC);
    }

    public List<Video> getTopRatedVideos() {
        // Since there's no averageRating field, we'll use like count as a proxy
        return videoRepository.findTop10ByPrivacyOrderByLikeCountDesc(Video.Privacy.PUBLIC);
    }

    public List<Video> getLatestVideos() {
        return videoRepository.findTop10ByPrivacyOrderByCreatedAtDesc(Video.Privacy.PUBLIC);
    }

    public Optional<Video> getVideoById(Long videoId) {
        return videoRepository.findById(videoId);
    }

    public Optional<Video> incrementViewCount(Long videoId) {
        Optional<Video> videoOpt = videoRepository.findById(videoId);
        if (videoOpt.isPresent()) {
            Video video = videoOpt.get();
            video.setViewCount(asLong(video.getViewCount()) + 1);
            return Optional.of(videoRepository.save(video));
        }
        return Optional.empty();
    }

    public Optional<Rating> findRatingForUser(Video video, User user) {
        return ratingRepository.findByVideoAndUser(video, user);
    }

    public Map<String, Object> toggleRating(Long videoId, User user, Rating.RatingType ratingType) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        Optional<Rating> existingOpt = ratingRepository.findByVideoAndUser(video, user);

        if (existingOpt.isPresent()) {
            Rating existing = existingOpt.get();
            if (existing.getRatingType() == ratingType) {
                ratingRepository.delete(existing);
            } else {
                existing.setRatingType(ratingType);
                ratingRepository.save(existing);
            }
        } else {
            Rating rating = new Rating(video, user, ratingType);
            ratingRepository.save(rating);
        }

        Long likeCount = ratingRepository.countByVideoAndRatingType(video, Rating.RatingType.LIKE);
        Long dislikeCount = ratingRepository.countByVideoAndRatingType(video, Rating.RatingType.DISLIKE);

        video.setLikeCount(likeCount != null ? likeCount : 0L);
        video.setDislikeCount(dislikeCount != null ? dislikeCount : 0L);
        videoRepository.save(video);

        Optional<Rating> postUpdate = ratingRepository.findByVideoAndUser(video, user);
        boolean liked = postUpdate.filter(r -> r.getRatingType() == Rating.RatingType.LIKE).isPresent();
        boolean disliked = postUpdate.filter(r -> r.getRatingType() == Rating.RatingType.DISLIKE).isPresent();

        Map<String, Object> response = new HashMap<>();
        response.put("liked", liked);
        response.put("disliked", disliked);
        response.put("likeCount", video.getLikeCount());
        response.put("dislikeCount", video.getDislikeCount());
        return response;
    }

    public List<Video> getVideosByUser(User user) {
        return videoRepository.findByUploadedByOrderByCreatedAtDesc(user);
    }

    public List<VideoDTO> getLikedVideos(User user) {
        if (user == null) {
            return List.of();
        }

        List<Rating> likes = ratingRepository.findByUserAndRatingTypeOrderByCreatedAtDesc(user, Rating.RatingType.LIKE);
        if (likes == null || likes.isEmpty()) {
            return List.of();
        }

        return likes.stream()
                .map(rating -> {
                    Video video = rating.getVideo();
                    if (video == null) {
                        return null;
                    }
                    VideoDTO dto = toDto(video, Optional.of(user));
                    if (dto != null && rating.getCreatedAt() != null) {
                        dto.setLikedAt(rating.getCreatedAt().toString());
                    }
                    return dto;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Video updateVideo(Long videoId, VideoDTO videoDTO, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        if (!video.getUploadedBy().getId().equals(user.getId())) {
            throw new IllegalStateException("Not authorized to update this video");
        }

        video.setTitle(videoDTO.getTitle());
        video.setDescription(videoDTO.getDescription());
    video.setTags(normalizeTags(videoDTO.getTags()));

        // Convert string privacy to enum
        if (videoDTO.getPrivacy() != null) {
            try {
                video.setPrivacy(Video.Privacy.valueOf(videoDTO.getPrivacy().toUpperCase()));
            } catch (IllegalArgumentException e) {
                video.setPrivacy(Video.Privacy.PUBLIC); // Default to PUBLIC if invalid
            }
        }

        // Update category if provided
        if (videoDTO.getCategoryId() != null) {
            categoryRepository.findById(videoDTO.getCategoryId())
                .ifPresent(video::setCategory);
        }

        return videoRepository.save(video);
    }

    /**
     * Delete a video and all its related files (video file and thumbnail)
     * Also cascades to delete all related data (comments, ratings, likes, etc.)
     * @param videoId The ID of the video to delete
     * @param user The user requesting the deletion (must be the owner)
     * @throws IllegalArgumentException if video not found
     * @throws IllegalStateException if user is not authorized
     */
    @Transactional
    public void deleteVideo(Long videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        // Check authorization
        if (!video.getUploadedBy().getId().equals(user.getId())) {
            throw new IllegalStateException("Not authorized to delete this video");
        }

        // Store file paths for deletion
        String videoFilePath = video.getFilepath();
        String thumbnailPath = video.getThumbnail();

        // Delete the video from database first
        // This will CASCADE delete all related data:
        // - Comments (and their likes and replies)
        // - Ratings
        // - Video likes and dislikes
        // - Playlist entries
        // - Watch history entries
        videoRepository.delete(video);

        // After successful database deletion, delete physical files
        deleteVideoFiles(videoFilePath, thumbnailPath);
        
        System.out.println("Video deleted successfully: ID=" + videoId + 
                         ", Title=" + video.getTitle() + 
                         ", Files cleaned up: " + (videoFilePath != null) + ", " + (thumbnailPath != null));
    }

    /**
     * Delete physical video and thumbnail files from the file system
     * @param videoFilePath The video file path
     * @param thumbnailPath The thumbnail file path
     */
    private void deleteVideoFiles(String videoFilePath, String thumbnailPath) {
        try {
            // Delete video file
            if (videoFilePath != null && !videoFilePath.isEmpty()) {
                Path videoPath = Paths.get(uploadDir, "videos", videoFilePath);
                if (Files.exists(videoPath)) {
                    Files.delete(videoPath);
                    System.out.println("Deleted video file: " + videoPath);
                } else {
                    System.out.println("Video file not found: " + videoPath);
                }
            }

            // Delete thumbnail file
            if (thumbnailPath != null && !thumbnailPath.isEmpty()) {
                Path thumbPath = Paths.get(uploadDir, "thumbnails", thumbnailPath);
                if (Files.exists(thumbPath)) {
                    Files.delete(thumbPath);
                    System.out.println("Deleted thumbnail file: " + thumbPath);
                } else {
                    System.out.println("Thumbnail file not found: " + thumbPath);
                }
            }
        } catch (IOException e) {
            // Log error but don't throw exception since database deletion was successful
            System.err.println("Error deleting video files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Delete multiple videos in batch (useful for admin or user account deletion)
     * @param videoIds List of video IDs to delete
     * @param user The user requesting the deletion
     * @return Number of videos successfully deleted
     */
    @Transactional
    public int deleteMultipleVideos(List<Long> videoIds, User user) {
        int deletedCount = 0;
        for (Long videoId : videoIds) {
            try {
                deleteVideo(videoId, user);
                deletedCount++;
            } catch (Exception e) {
                System.err.println("Failed to delete video " + videoId + ": " + e.getMessage());
            }
        }
        return deletedCount;
    }

    public List<Video> searchVideos(String keyword) {
        return searchVideos(keyword, null);
    }

    public List<Video> searchVideos(String keyword, String categoryName) {
        String trimmedKeyword = keyword != null ? keyword.trim() : "";

        List<Video> baseResults;
        if (trimmedKeyword.isEmpty()) {
            baseResults = getAllPublicVideos();
        } else {
            // Search only in title
            baseResults = videoRepository.findByTitleContainingIgnoreCase(trimmedKeyword);
        }

        return baseResults.stream()
            .filter(video -> video.getPrivacy() == null || video.getPrivacy() == Video.Privacy.PUBLIC)
            .filter(video -> categoryName == null || categoryName.isBlank() ||
                (video.getCategory() != null && categoryName.equalsIgnoreCase(video.getCategory().getName())))
            .collect(Collectors.toList());
    }

    public List<Video> getVideosByCategory(Long categoryId) {
        return videoRepository.findByCategoryIdAndPrivacyOrderByCreatedAtDesc(categoryId, Video.Privacy.PUBLIC);
    }

    public List<Video> getRelatedVideos(Long videoId) {
        Optional<Video> videoOpt = videoRepository.findById(videoId);
        if (videoOpt.isEmpty()) {
            return videoRepository.findTop5ByPrivacyOrderByViewCountDesc(Video.Privacy.PUBLIC);
        }

        Video video = videoOpt.get();
        LinkedHashSet<Video> related = new LinkedHashSet<>();

        List<String> tagList = extractTags(video.getTags());
        for (String tag : tagList) {
            if (related.size() >= 10) {
                break;
            }
            videoRepository
                .findTop10ByTagsContainingIgnoreCaseAndIdNotAndPrivacyOrderByViewCountDesc(tag, videoId, Video.Privacy.PUBLIC)
                .forEach(related::add);
        }

        if (video.getCategory() != null) {
            videoRepository
                .findTop5ByCategoryIdAndIdNotAndPrivacyOrderByViewCountDesc(video.getCategory().getId(), videoId, Video.Privacy.PUBLIC)
                .forEach(related::add);
        }

        if (related.isEmpty()) {
            videoRepository.findTop5ByPrivacyOrderByViewCountDesc(Video.Privacy.PUBLIC).stream()
                .filter(v -> !v.getId().equals(videoId))
                .forEach(related::add);
        }

        return related.stream()
            .filter(v -> !v.getId().equals(videoId))
            .limit(10)
            .collect(Collectors.toList());
    }

    // Advanced filtering methods
    public List<Video> getFilteredVideos(String category, String duration, String uploadDate,
                                       Double minRating, Long minViews, String search, String sortBy) {
        // This is a simplified implementation. In a real application, you might want to use
        // JPA Criteria API or custom query methods for more complex filtering
        List<Video> videos = getAllPublicVideos();

        // Apply filters (this would be better implemented with database queries)
        return videos.stream()
            .filter(video -> category == null ||
                    (video.getCategory() != null && video.getCategory().getName().equalsIgnoreCase(category)))
            .filter(video -> minRating == null ||
                    (video.getLikeCount() != null && video.getLikeCount() >= minRating.longValue())) // Use like count as proxy for rating
            .filter(video -> minViews == null ||
                    (video.getViewCount() != null && video.getViewCount() >= minViews))
            .filter(video -> search == null || search.isEmpty() ||
                    video.getTitle().toLowerCase().contains(search.toLowerCase()))
            .sorted((v1, v2) -> {
                return switch (sortBy.toLowerCase()) {
                    case "title-asc" -> v1.getTitle().compareToIgnoreCase(v2.getTitle());
                    case "title-desc" -> v2.getTitle().compareToIgnoreCase(v1.getTitle());
                    case "views-desc" -> Long.compare(asLong(v2.getViewCount()), asLong(v1.getViewCount()));
                    case "views-asc" -> Long.compare(asLong(v1.getViewCount()), asLong(v2.getViewCount()));
                    case "rating-desc" -> Long.compare(asLong(v2.getLikeCount()), asLong(v1.getLikeCount()));
                    case "rating-asc" -> Long.compare(asLong(v1.getLikeCount()), asLong(v2.getLikeCount()));
                    case "oldest" -> v1.getCreatedAt().compareTo(v2.getCreatedAt());
                    case "latest" -> v2.getCreatedAt().compareTo(v1.getCreatedAt());
                    default -> v2.getCreatedAt().compareTo(v1.getCreatedAt());
                };
            })
            .toList();
    }

    public List<Video> getVideosByDurationRange(int minDuration, int maxDuration) {
        return videoRepository.findByDurationBetweenAndPrivacyOrderByCreatedAtDesc(minDuration, maxDuration, Video.Privacy.PUBLIC);
    }

    public List<Video> getVideosByMinRating(double minRating) {
        // Use like count as proxy for rating since there's no averageRating field
        return videoRepository.findByLikeCountGreaterThanEqualAndPrivacyOrderByLikeCountDesc((long)minRating, Video.Privacy.PUBLIC);
    }

    public List<Video> getVideosByMinViews(long minViews) {
        return videoRepository.findByViewCountGreaterThanEqualAndPrivacyOrderByViewCountDesc(minViews, Video.Privacy.PUBLIC);
    }

    public List<Video> getVideosByDateRange(String startDate, String endDate) {
        // This would need proper date parsing in a real implementation
        return videoRepository.findByPrivacyOrderByCreatedAtDesc(Video.Privacy.PUBLIC);
    }

    public List<Video> getTrendingVideosByCategory(Long categoryId) {
        return videoRepository.findTop10ByCategoryIdAndPrivacyOrderByViewCountDesc(categoryId, Video.Privacy.PUBLIC);
    }

    private long asLong(Long value) {
        return value != null ? value : 0L;
    }

    private List<String> extractTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return List.of();
        }

        List<String> tags = new ArrayList<>();

        try {
            JsonNode node = objectMapper.readTree(rawTags);
            if (node.isArray()) {
                node.forEach(child -> {
                    String value = child.asText(null);
                    if (value != null) {
                        String normalized = value.trim().toLowerCase();
                        if (!normalized.isEmpty() && !tags.contains(normalized)) {
                            tags.add(normalized);
                        }
                    }
                });
                return tags;
            }
        } catch (JsonProcessingException ignored) {
            // Fall through to string parsing
        }

        Arrays.stream(rawTags.split(","))
            .map(part -> part.replaceAll("[\\[\\]{}\"]", " "))
            .map(String::trim)
            .filter(tag -> !tag.isEmpty())
            .map(String::toLowerCase)
            .forEach(tag -> {
                if (!tags.contains(tag)) {
                    tags.add(tag);
                }
            });

        return tags;
    }

    private String normalizeTags(String rawTags) {
        if (rawTags == null) {
            return null;
        }

        String trimmed = rawTags.trim();
        if (trimmed.isEmpty()) {
            return "[]";
        }

        try {
            if (trimmed.startsWith("[")) {
                objectMapper.readTree(trimmed);
                return trimmed;
            }
        } catch (JsonProcessingException ignored) {
            // Fall through to rebuilding tags list
        }

        List<String> tagList = Arrays.stream(trimmed.split(","))
            .map(part -> part.replaceAll("[\\[\\]\\{\\}\"]", " "))
            .map(part -> part.replace(':', ' '))
            .map(String::trim)
            .flatMap(part -> Arrays.stream(part.split("\\s+")))
            .map(String::trim)
            .filter(tag -> !tag.isEmpty())
            .collect(Collectors.toList());

        if (tagList.isEmpty()) {
            return "[]";
        }

        try {
            return objectMapper.writeValueAsString(tagList);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    public VideoRatingSummary getVideoRatingSummary(Long videoId, Optional<User> currentUser) {
        Video video = videoRepository.findById(videoId)
            .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        return buildRatingSummary(video, currentUser);
    }

    public VideoRatingSummary saveUserRating(Long videoId, User user, int score) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Video video = videoRepository.findById(videoId)
            .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        VideoUserRating rating = videoUserRatingRepository
            .findByVideoIdAndUserId(videoId, user.getId())
            .orElseGet(() -> new VideoUserRating(video, user, score));

        rating.setScore(score);
        videoUserRatingRepository.save(rating);

        return buildRatingSummary(video, Optional.ofNullable(user));
    }

    private VideoRatingSummary buildRatingSummary(Video video, Optional<User> currentUser) {
        if (video == null) {
            return new VideoRatingSummary(0.0, 0L, null);
        }

        Double average = videoUserRatingRepository.findAverageScoreByVideoId(video.getId());
        long count = videoUserRatingRepository.countByVideoId(video.getId());

        Integer userScore = currentUser
            .flatMap(user -> videoUserRatingRepository.findByVideoIdAndUserId(video.getId(), user.getId()))
            .map(VideoUserRating::getScore)
            .orElse(null);

        double roundedAverage = average != null ? Math.round(average * 10.0) / 10.0 : 0.0;
        return new VideoRatingSummary(roundedAverage, count, userScore);
    }

    private String encodeForUrl(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }

        String trimmed = path.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        String normalized = trimmed.replace("\\", "/");
        boolean leadingSlash = normalized.startsWith("/");
        String working = leadingSlash ? normalized.substring(1) : normalized;

        String encoded = Arrays.stream(working.split("/"))
                .filter(segment -> segment != null && !segment.isBlank())
                .map(this::encodeSegment)
                .collect(Collectors.joining("/"));

        return leadingSlash ? "/" + encoded : encoded;
    }

    private String encodeSegment(String segment) {
        String toEncode = segment;
        try {
            toEncode = URLDecoder.decode(segment, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            // segment was not percent-encoded; proceed with original value
        }
        return UriUtils.encodePathSegment(toEncode, StandardCharsets.UTF_8);
    }
}




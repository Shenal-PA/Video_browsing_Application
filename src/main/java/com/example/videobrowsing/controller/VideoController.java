package com.example.videobrowsing.controller;



import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.videobrowsing.dto.CommentDTO;
import com.example.videobrowsing.dto.VideoDTO;
import com.example.videobrowsing.dto.VideoRatingSummary;
import com.example.videobrowsing.entity.Rating;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.service.CommentService;
import com.example.videobrowsing.service.UserService;
import com.example.videobrowsing.service.VideoService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "*")
public class VideoController {

    private static final Logger log = LoggerFactory.getLogger(VideoController.class);

    @Autowired
    private VideoService videoService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;


    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("privacy") String privacy,
            @RequestParam("tags") String tags,
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
            HttpSession session) {

        Optional<User> userOpt = resolveSessionUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        try {
            User user = userOpt.get();

            VideoDTO videoDTO = new VideoDTO();
            videoDTO.setTitle(title);
            videoDTO.setDescription(description);
            videoDTO.setCategoryId(categoryId);
            videoDTO.setPrivacy(privacy);
            videoDTO.setTags(tags);

            Video video = videoService.uploadVideo(videoDTO, videoFile, thumbnailFile, user);
            return ResponseEntity.ok(videoService.toDto(video, Optional.of(user)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            log.error("Error uploading video", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process video upload");
        }
    }

    @GetMapping
    public ResponseEntity<List<VideoDTO>> listVideos(
            @RequestParam(value = "sort", defaultValue = "latest") String sort,
            @RequestParam(value = "limit", required = false) Integer limit,
            HttpSession session) {

        Optional<User> currentUser = resolveSessionUser(session);
        List<Video> videos = selectVideosBySort(sort);

        if (limit != null && limit > 0 && limit < videos.size()) {
            videos = videos.stream().limit(limit).collect(Collectors.toList());
        }

    return ResponseEntity.ok(videoService.toDtos(videos, currentUser));
    }

    @GetMapping("/public")
    public ResponseEntity<List<VideoDTO>> getPublicVideos(HttpSession session) {
        Optional<User> currentUser = resolveSessionUser(session);
    List<Video> videos = videoService.getAllPublicVideos();
    return ResponseEntity.ok(videoService.toDtos(videos, currentUser));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<List<VideoDTO>> getTopRatedVideos(HttpSession session) {
        Optional<User> currentUser = resolveSessionUser(session);
    List<Video> videos = videoService.getTopRatedVideos();
    return ResponseEntity.ok(videoService.toDtos(videos, currentUser));
    }

    @GetMapping("/latest")
    public ResponseEntity<List<VideoDTO>> getLatestVideos(HttpSession session) {
        Optional<User> currentUser = resolveSessionUser(session);
    List<Video> videos = videoService.getLatestVideos();
    return ResponseEntity.ok(videoService.toDtos(videos, currentUser));
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<?> getVideo(@PathVariable Long videoId, HttpSession session) {
        Optional<Video> videoOpt = videoService.getVideoById(videoId);
        if (videoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Video not found");
        }

    Video video = videoService.incrementViewCount(videoId).orElse(videoOpt.get());
    Optional<User> currentUser = resolveSessionUser(session);

    VideoDTO dto = videoService.toDto(video, currentUser);
    return ResponseEntity.ok(dto);
    }

    @GetMapping("/my-videos")
    public ResponseEntity<?> getMyVideos(HttpSession session) {
        Optional<User> userOpt = resolveSessionUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Not logged in");
        }

    List<Video> videos = videoService.getVideosByUser(userOpt.get());
    return ResponseEntity.ok(videoService.toDtos(videos, userOpt));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserVideos(@PathVariable Long userId, HttpSession session) {
        Optional<User> targetUser = userService.findById(userId);
        if (targetUser.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        Optional<User> currentUser = resolveSessionUser(session);
        List<Video> videos = videoService.getVideosByUser(targetUser.get());
        return ResponseEntity.ok(videoService.toDtos(videos, currentUser));
    }

    @GetMapping("/liked")
    public ResponseEntity<?> getLikedVideos(HttpSession session) {
        Optional<User> userOpt = resolveSessionUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        List<VideoDTO> likedVideos = videoService.getLikedVideos(userOpt.get());
        return ResponseEntity.ok(likedVideos);
    }

    @PutMapping("/{videoId}")
    public ResponseEntity<?> updateVideo(@PathVariable Long videoId, @RequestBody VideoDTO videoDTO, HttpSession session) {
        Optional<User> userOpt = resolveSessionUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        try {
            Video updatedVideo = videoService.updateVideo(videoId, videoDTO, userOpt.get());
            return ResponseEntity.ok(videoService.toDto(updatedVideo, userOpt));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<?> deleteVideo(@PathVariable Long videoId, HttpSession session) {
        Optional<User> userOpt = resolveSessionUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        try {
            videoService.deleteVideo(videoId, userOpt.get());
            return ResponseEntity.ok("Video deleted successfully");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<VideoDTO>> searchVideos(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            HttpSession session) {

    Optional<User> currentUser = resolveSessionUser(session);
    List<Video> videos = videoService.searchVideos(keyword, category);
    return ResponseEntity.ok(videoService.toDtos(videos, currentUser));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<VideoDTO>> getVideosByCategory(@PathVariable Long categoryId, HttpSession session) {
    Optional<User> currentUser = resolveSessionUser(session);
    List<Video> videos = videoService.getVideosByCategory(categoryId);
    return ResponseEntity.ok(videoService.toDtos(videos, currentUser));
    }

    @GetMapping("/{videoId}/related")
    public ResponseEntity<List<VideoDTO>> getRelatedVideos(@PathVariable Long videoId, HttpSession session) {
    Optional<User> currentUser = resolveSessionUser(session);
    List<Video> videos = videoService.getRelatedVideos(videoId);
    return ResponseEntity.ok(videoService.toDtos(videos, currentUser));
    }

    @GetMapping("/{videoId}/rating")
    public ResponseEntity<?> getVideoRating(@PathVariable Long videoId, HttpSession session) {
        Optional<User> currentUser = resolveSessionUser(session);
        try {
            VideoRatingSummary summary = videoService.getVideoRatingSummary(videoId, currentUser);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/{videoId}/rating")
    public ResponseEntity<?> rateVideo(@PathVariable Long videoId,
                                       @RequestBody Map<String, Integer> payload,
                                       HttpSession session) {
        Optional<User> userOpt = resolveSessionUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        Integer score = payload != null ? payload.get("score") : null;
        if (score == null) {
            return ResponseEntity.badRequest().body("Score is required");
        }

        try {
            VideoRatingSummary summary = videoService.saveUserRating(videoId, userOpt.get(), score);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/filtered")
    public ResponseEntity<List<VideoDTO>> getFilteredVideos(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String duration,
            @RequestParam(required = false) String uploadDate,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Long minViews,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "latest") String sortBy,
            HttpSession session) {

        Optional<User> currentUser = resolveSessionUser(session);
        List<Video> videos = videoService.getFilteredVideos(
            category, duration, uploadDate, minRating, minViews, search, sortBy);
        return ResponseEntity.ok(videoService.toDtos(videos, currentUser));
    }

    @GetMapping("/by-duration")
    public ResponseEntity<List<VideoDTO>> getVideosByDuration(
            @RequestParam int minDuration,
            @RequestParam int maxDuration,
            HttpSession session) {
    Optional<User> currentUser = resolveSessionUser(session);
    List<Video> videos = videoService.getVideosByDurationRange(minDuration, maxDuration);
    return ResponseEntity.ok(videoService.toDtos(videos, currentUser));
    }

    @GetMapping("/by-rating")
    public ResponseEntity<List<VideoDTO>> getVideosByRating(@RequestParam double minRating, HttpSession session) {
    Optional<User> currentUser = resolveSessionUser(session);
    List<Video> videos = videoService.getVideosByMinRating(minRating);
    return ResponseEntity.ok(videoService.toDtos(videos, currentUser));
    }

    @GetMapping("/by-views")
    public ResponseEntity<List<VideoDTO>> getVideosByViews(@RequestParam long minViews, HttpSession session) {
    Optional<User> currentUser = resolveSessionUser(session);
    List<Video> videos = videoService.getVideosByMinViews(minViews);
    return ResponseEntity.ok(videoService.toDtos(videos, currentUser));
    }

    @GetMapping("/by-date-range")
    public ResponseEntity<List<VideoDTO>> getVideosByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            HttpSession session) {
    Optional<User> currentUser = resolveSessionUser(session);
    List<Video> videos = videoService.getVideosByDateRange(startDate, endDate);
    return ResponseEntity.ok(videoService.toDtos(videos, currentUser));
    }

    @GetMapping("/trending-by-category/{categoryId}")
    public ResponseEntity<List<VideoDTO>> getTrendingVideosByCategory(@PathVariable Long categoryId, HttpSession session) {
    Optional<User> currentUser = resolveSessionUser(session);
    List<Video> videos = videoService.getTrendingVideosByCategory(categoryId);
    return ResponseEntity.ok(videoService.toDtos(videos, currentUser));
    }

    @PostMapping("/{videoId}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long videoId, HttpSession session) {
        return handleRatingToggle(videoId, session, Rating.RatingType.LIKE);
    }

    @PostMapping("/{videoId}/dislike")
    public ResponseEntity<?> toggleDislike(@PathVariable Long videoId, HttpSession session) {
        return handleRatingToggle(videoId, session, Rating.RatingType.DISLIKE);
    }

    @GetMapping("/{videoId}/comments")
    public ResponseEntity<List<CommentDTO>> getVideoComments(@PathVariable Long videoId, HttpSession session) {
        Optional<User> userOpt = resolveSessionUser(session);
        return ResponseEntity.ok(commentService.getCommentDtosForVideo(videoId, userOpt.orElse(null)));
    }

    @PostMapping("/{videoId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long videoId, @RequestBody CommentDTO commentDTO, HttpSession session) {
        if (commentDTO.getContent() == null || commentDTO.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Comment content cannot be empty");
        }

        Optional<User> userOpt = resolveSessionUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        try {
            CommentDTO saved = commentService.createComment(videoId, userOpt.get().getId(), commentDTO.getContent(), commentDTO.getParentCommentId());
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{videoId}/comments/{commentId}/like")
    public ResponseEntity<?> toggleCommentLike(@PathVariable Long videoId,
                                               @PathVariable Long commentId,
                                               HttpSession session) {
        Optional<User> userOpt = resolveSessionUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        try {
            Map<String, Object> payload = commentService.toggleCommentLike(videoId, commentId, userOpt.get());
            return ResponseEntity.ok(payload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{videoId}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long videoId, @PathVariable Long commentId, HttpSession session) {
        Optional<User> userOpt = resolveSessionUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        try {
            commentService.deleteComment(videoId, commentId, userOpt.get());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    private ResponseEntity<?> handleRatingToggle(Long videoId, HttpSession session, Rating.RatingType ratingType) {
        Optional<User> userOpt = resolveSessionUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        Map<String, Object> payload = videoService.toggleRating(videoId, userOpt.get(), ratingType);
        return ResponseEntity.ok(payload);
    }

    private List<Video> selectVideosBySort(String sort) {
        String normalized = sort == null ? "" : sort.trim().toLowerCase();
        return switch (normalized) {
            case "views", "popular" -> videoService.getMostViewedVideos();
            case "likes", "top", "trending" -> videoService.getMostLikedVideos();
            case "createdat", "latest", "newest" -> videoService.getAllPublicVideos();
            default -> videoService.getAllPublicVideos();
        };
    }

    private Optional<User> resolveSessionUser(HttpSession session) {
        return userService.resolveCurrentUser(session);
    }
}

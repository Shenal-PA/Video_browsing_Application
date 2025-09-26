package com.example.videobrowsing.controller;



import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.dto.VideoDTO;
import com.example.videobrowsing.service.VideoService;
import com.example.videobrowsing.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
        import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "*")
public class VideoController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private UserService userService;

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

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body("Not logged in");
        }

        try {
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("User not found");
            }

            User user = userOpt.get();

            VideoDTO videoDTO = new VideoDTO();
            videoDTO.setTitle(title);
            videoDTO.setDescription(description);
            videoDTO.setCategoryId(categoryId);
            videoDTO.setPrivacy(privacy);
            videoDTO.setTags(tags);

            Video video = videoService.uploadVideo(videoDTO, videoFile, thumbnailFile, user);
            return ResponseEntity.ok(video);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/public")
    public ResponseEntity<List<Video>> getPublicVideos() {
        List<Video> videos = videoService.getAllPublicVideos();
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/top-rated")
    public ResponseEntity<List<Video>> getTopRatedVideos() {
        List<Video> videos = videoService.getTopRatedVideos();
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/latest")
    public ResponseEntity<List<Video>> getLatestVideos() {
        List<Video> videos = videoService.getLatestVideos();
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<?> getVideo(@PathVariable Long videoId, HttpSession session) {
        Optional<Video> video = videoService.getVideoById(videoId);
        if (video.isPresent()) {
            // Increment view count
            videoService.incrementViewCount(videoId);
            return ResponseEntity.ok(video.get());
        }
        return ResponseEntity.badRequest().body("Video not found");
    }

    @GetMapping("/my-videos")
    public ResponseEntity<?> getMyVideos(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body("Not logged in");
        }

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isPresent()) {
            List<Video> videos = videoService.getVideosByUser(userOpt.get());
            return ResponseEntity.ok(videos);
        }
        return ResponseEntity.badRequest().body("User not found");
    }

    @PutMapping("/{videoId}")
    public ResponseEntity<?> updateVideo(@PathVariable Long videoId, @RequestBody VideoDTO videoDTO, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body("Not logged in");
        }

        try {
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isPresent()) {
                Video updatedVideo = videoService.updateVideo(videoId, videoDTO, userOpt.get());
                return ResponseEntity.ok(updatedVideo);
            }
            return ResponseEntity.badRequest().body("User not found");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<?> deleteVideo(@PathVariable Long videoId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body("Not logged in");
        }

        try {
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isPresent()) {
                videoService.deleteVideo(videoId, userOpt.get());
                return ResponseEntity.ok("Video deleted successfully");
            }
            return ResponseEntity.badRequest().body("User not found");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Video>> searchVideos(@RequestParam String keyword) {
        List<Video> videos = videoService.searchVideos(keyword);
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Video>> getVideosByCategory(@PathVariable Long categoryId) {
        List<Video> videos = videoService.getVideosByCategory(categoryId);
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/{videoId}/related")
    public ResponseEntity<List<Video>> getRelatedVideos(@PathVariable Long videoId) {
        List<Video> videos = videoService.getRelatedVideos(videoId);
        return ResponseEntity.ok(videos);
    }
}

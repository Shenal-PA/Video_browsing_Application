package com.example.videobrowsing.service;



import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.entity.Category;
import com.example.videobrowsing.dto.VideoDTO;
import com.example.videobrowsing.repository.VideoRepository;
import com.example.videobrowsing.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class VideoService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public Video uploadVideo(VideoDTO videoDTO, MultipartFile videoFile, MultipartFile thumbnailFile, User uploader) throws IOException {
        // Save video file
        String videoFileName = UUID.randomUUID().toString() + "_" + videoFile.getOriginalFilename();
        Path videoPath = Paths.get(uploadDir, "videos", videoFileName);
        Files.createDirectories(videoPath.getParent());
        Files.write(videoPath, videoFile.getBytes());

        // Save thumbnail file if provided
        String thumbnailFileName = null;
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            thumbnailFileName = UUID.randomUUID().toString() + "_" + thumbnailFile.getOriginalFilename();
            Path thumbnailPath = Paths.get(uploadDir, "thumbnails", thumbnailFileName);
            Files.createDirectories(thumbnailPath.getParent());
            Files.write(thumbnailPath, thumbnailFile.getBytes());
        }

        Video video = new Video();
        video.setTitle(videoDTO.getTitle());
        video.setDescription(videoDTO.getDescription());
        video.setFilePath("/uploads/videos/" + videoFileName);
        if (thumbnailFileName != null) {
            video.setThumbnailPath("/uploads/thumbnails/" + thumbnailFileName);
        }
        video.setFileSize(videoFile.getSize());
        video.setUploadedBy(uploader);
        video.setTags(videoDTO.getTags());

        if (videoDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(videoDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            video.setCategory(category);
        }

        if (videoDTO.getPrivacy() != null) {
            video.setPrivacy(Video.Privacy.valueOf(videoDTO.getPrivacy()));
        }

        video.setStatus(Video.Status.PUBLISHED);

        return videoRepository.save(video);
    }

    public List<Video> getAllPublicVideos() {
        return videoRepository.findPublicVideos();
    }

    public List<Video> getTopRatedVideos() {
        return videoRepository.findTopRatedVideos();
    }

    public List<Video> getLatestVideos() {
        return videoRepository.findLatestVideos();
    }

    public Optional<Video> getVideoById(Long id) {
        return videoRepository.findById(id);
    }

    public List<Video> getVideosByUser(User user) {
        return videoRepository.findByUploadedBy(user);
    }

    public List<Video> searchVideos(String keyword) {
        return videoRepository.searchVideos(keyword);
    }

    public List<Video> getVideosByCategory(Long categoryId) {
        return videoRepository.findByCategoryId(categoryId);
    }

    public List<Video> getRelatedVideos(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        return videoRepository.findRelatedVideos(video.getCategory(), videoId);
    }

    public Video updateVideo(Long videoId, VideoDTO videoDTO, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        // Check if user owns the video or is admin
        if (!video.getUploadedBy().getId().equals(user.getId()) && !user.getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Not authorized to update this video");
        }

        if (videoDTO.getTitle() != null) video.setTitle(videoDTO.getTitle());
        if (videoDTO.getDescription() != null) video.setDescription(videoDTO.getDescription());
        if (videoDTO.getTags() != null) video.setTags(videoDTO.getTags());
        if (videoDTO.getPrivacy() != null) video.setPrivacy(Video.Privacy.valueOf(videoDTO.getPrivacy()));

        if (videoDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(videoDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            video.setCategory(category);
        }

        return videoRepository.save(video);
    }

    public void deleteVideo(Long videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        // Check if user owns the video or is admin
        if (!video.getUploadedBy().getId().equals(user.getId()) && !user.getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Not authorized to delete this video");
        }

        videoRepository.delete(video);
    }

    public Video incrementViewCount(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        video.setViewCount(video.getViewCount() + 1);
        return videoRepository.save(video);
    }

    public Long getVideoCountByStatus(Video.Status status) {
        return videoRepository.countByStatus(status);
    }

    public Video updateVideoStatus(Long videoId, String status) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        video.setStatus(Video.Status.valueOf(status));
        return videoRepository.save(video);
    }
}
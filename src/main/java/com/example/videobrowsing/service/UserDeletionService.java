package com.example.videobrowsing.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.repository.UserRepository;

/**
 * Service for handling user deletion with complete cleanup of:
 * - Profile photos
 * - All user's videos and their files
 * - All related database records (via CASCADE)
 */
@Service
public class UserDeletionService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoService videoService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Delete a user and all their data (videos, comments, photos, etc.)
     * @param userId The ID of the user to delete
     * @param requestingUser The user requesting the deletion (for authorization)
     * @throws IllegalArgumentException if user not found
     * @throws IllegalStateException if not authorized
     */
    @Transactional
    public void deleteUserAccount(Long userId, User requestingUser) {
        // Check if user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check authorization (user can only delete their own account, unless admin)
        if (!user.getId().equals(requestingUser.getId()) && 
            !"ADMIN".equals(requestingUser.getRole())) {
            throw new IllegalStateException("Not authorized to delete this account");
        }

        System.out.println("=== STARTING USER DELETION ===");
        System.out.println("User ID: " + userId);
        System.out.println("Username: " + user.getUsername());
        System.out.println("Email: " + user.getEmail());

        // Step 1: Get all user's videos before deletion
        List<Video> userVideos = videoService.getVideosByUser(user);
        System.out.println("Found " + userVideos.size() + " videos to delete");

        // Step 2: Delete all user's videos (and their files)
        int videosDeleted = 0;
        for (Video video : userVideos) {
            try {
                videoService.deleteVideo(video.getId(), user);
                videosDeleted++;
            } catch (Exception e) {
                System.err.println("Error deleting video " + video.getId() + ": " + e.getMessage());
            }
        }
        System.out.println("Deleted " + videosDeleted + " out of " + userVideos.size() + " videos");

        // Step 3: Delete user's profile photo
        deleteUserPhoto(user.getProfilePicture());

        // Step 4: Delete user from database
        // This will CASCADE delete all remaining related data:
        // - Comments (and their likes and replies)
        // - Ratings
        // - Video likes/dislikes
        // - Comment likes
        // - Subscriptions (as subscriber and creator)
        // - Playlists (and playlist videos)
        // - Watch history
        userRepository.delete(user);

        System.out.println("=== USER DELETION COMPLETED ===");
        System.out.println("User account deleted: " + user.getUsername());
    }

    /**
     * Delete user's profile photo from file system
     * @param photoPath The profile photo path
     */
    private void deleteUserPhoto(String photoPath) {
        if (photoPath == null || photoPath.isEmpty() || 
            photoPath.equals("/images/default-avatar.png") ||
            photoPath.startsWith("http")) {
            System.out.println("No custom profile photo to delete (using default or external)");
            return;
        }

        try {
            // Handle different photo path formats
            String cleanPath = photoPath;
            if (cleanPath.startsWith("/uploads/")) {
                cleanPath = cleanPath.substring("/uploads/".length());
            } else if (cleanPath.startsWith("uploads/")) {
                cleanPath = cleanPath.substring("uploads/".length());
            }

            Path photoFile = Paths.get(uploadDir, cleanPath);
            if (Files.exists(photoFile)) {
                Files.delete(photoFile);
                System.out.println("Deleted profile photo: " + photoFile);
            } else {
                System.out.println("Profile photo file not found: " + photoFile);
            }
        } catch (IOException e) {
            System.err.println("Error deleting profile photo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get statistics about what will be deleted (preview before deletion)
     * @param userId The user ID
     * @return Map with deletion statistics
     */
    public DeletionPreview getDeletePreview(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        DeletionPreview preview = new DeletionPreview();
        preview.setUsername(user.getUsername());
        preview.setEmail(user.getEmail());
        
        // Count videos
        List<Video> videos = videoService.getVideosByUser(user);
        preview.setVideosCount(videos.size());
        
        // Estimate file sizes
        long totalVideoSize = videos.stream()
                .mapToLong(v -> v.getFileSize() != null ? v.getFileSize() : 0)
                .sum();
        preview.setTotalVideoSize(totalVideoSize);
        
        preview.setHasProfilePhoto(user.getProfilePicture() != null && 
                                   !user.getProfilePicture().contains("default"));

        return preview;
    }

    /**
     * Inner class for deletion preview
     */
    public static class DeletionPreview {
        private String username;
        private String email;
        private int videosCount;
        private long totalVideoSize;
        private boolean hasProfilePhoto;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public int getVideosCount() { return videosCount; }
        public void setVideosCount(int videosCount) { this.videosCount = videosCount; }
        
        public long getTotalVideoSize() { return totalVideoSize; }
        public void setTotalVideoSize(long totalVideoSize) { this.totalVideoSize = totalVideoSize; }
        
        public boolean isHasProfilePhoto() { return hasProfilePhoto; }
        public void setHasProfilePhoto(boolean hasProfilePhoto) { this.hasProfilePhoto = hasProfilePhoto; }

        @Override
        public String toString() {
            return String.format(
                "User: %s (%s) - Videos: %d, Total Size: %.2f MB, Has Photo: %s",
                username, email, videosCount, totalVideoSize / 1024.0 / 1024.0, hasProfilePhoto
            );
        }
    }
}

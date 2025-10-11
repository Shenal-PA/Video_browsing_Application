package com.example.videobrowsing.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.entity.Video;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    // Basic queries using Privacy enum and correct field names
    List<Video> findByPrivacyOrderByCreatedAtDesc(Video.Privacy privacy);
       List<Video> findByPrivacyOrderByViewCountDesc(Video.Privacy privacy);
       List<Video> findByPrivacyOrderByLikeCountDesc(Video.Privacy privacy);
    List<Video> findByUploadedByOrderByCreatedAtDesc(User uploadedBy);
    List<Video> findByCategoryIdAndPrivacyOrderByCreatedAtDesc(Long categoryId, Video.Privacy privacy);

    // Top rated videos (using like count as proxy)
    List<Video> findTop10ByPrivacyOrderByLikeCountDesc(Video.Privacy privacy);

    // Latest videos
    List<Video> findTop10ByPrivacyOrderByCreatedAtDesc(Video.Privacy privacy);

    // Most viewed videos
    List<Video> findTop5ByPrivacyOrderByViewCountDesc(Video.Privacy privacy);
    List<Video> findTop10ByCategoryIdAndPrivacyOrderByViewCountDesc(Long categoryId, Video.Privacy privacy);

    // Search functionality - search only in title
    List<Video> findByTitleContainingIgnoreCase(String title);

    // Related videos
    List<Video> findTop5ByCategoryIdAndIdNotAndPrivacyOrderByViewCountDesc(Long categoryId, Long excludeId, Video.Privacy privacy);
       List<Video> findTop10ByTagsContainingIgnoreCaseAndIdNotAndPrivacyOrderByViewCountDesc(String tag, Long excludeId, Video.Privacy privacy);

    // Duration-based queries
    List<Video> findByDurationBetweenAndPrivacyOrderByCreatedAtDesc(Integer minDuration, Integer maxDuration, Video.Privacy privacy);
    List<Video> findByDurationLessThanAndPrivacyOrderByCreatedAtDesc(Integer maxDuration, Video.Privacy privacy);
    List<Video> findByDurationGreaterThanAndPrivacyOrderByCreatedAtDesc(Integer minDuration, Video.Privacy privacy);

    // Like count-based queries (as proxy for rating)
    List<Video> findByLikeCountGreaterThanEqualAndPrivacyOrderByLikeCountDesc(Long minLikes, Video.Privacy privacy);
    List<Video> findByLikeCountBetweenAndPrivacyOrderByLikeCountDesc(Long minLikes, Long maxLikes, Video.Privacy privacy);

    // View count-based queries
    List<Video> findByViewCountGreaterThanEqualAndPrivacyOrderByViewCountDesc(Long minViews, Video.Privacy privacy);
    List<Video> findByViewCountBetweenAndPrivacyOrderByViewCountDesc(Long minViews, Long maxViews, Video.Privacy privacy);

    // Date-based queries using createdAt field
    List<Video> findByCreatedAtAfterAndPrivacyOrderByCreatedAtDesc(LocalDateTime date, Video.Privacy privacy);
    List<Video> findByCreatedAtBetweenAndPrivacyOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate, Video.Privacy privacy);

    // Complex queries using @Query annotation
    @Query("SELECT v FROM Video v WHERE v.privacy = :privacy AND " +
           "(:category IS NULL OR v.category.name = :category) AND " +
           "(:minLikes IS NULL OR v.likeCount >= :minLikes) AND " +
           "(:minViews IS NULL OR v.viewCount >= :minViews) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(v.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY v.createdAt DESC")
    List<Video> findFilteredVideos(@Param("privacy") Video.Privacy privacy,
                                 @Param("category") String category,
                                 @Param("minLikes") Long minLikes,
                                 @Param("minViews") Long minViews,
                                 @Param("search") String search);

    // Trending videos (most views in recent time)
    @Query("SELECT v FROM Video v WHERE v.privacy = :privacy AND v.createdAt > :since " +
           "ORDER BY v.viewCount DESC")
    List<Video> findTrendingVideos(@Param("privacy") Video.Privacy privacy, @Param("since") LocalDateTime since);

    // Popular videos by category using like count and view count
    @Query("SELECT v FROM Video v WHERE v.privacy = :privacy AND v.category.id = :categoryId " +
           "ORDER BY (v.viewCount * 0.7 + COALESCE(v.likeCount, 0) * 0.3) DESC")
    List<Video> findPopularVideosByCategory(@Param("privacy") Video.Privacy privacy, @Param("categoryId") Long categoryId);

    // Recently uploaded videos
    @Query("SELECT v FROM Video v WHERE v.privacy = :privacy AND v.createdAt > :since " +
           "ORDER BY v.createdAt DESC")
    List<Video> findRecentVideos(@Param("privacy") Video.Privacy privacy, @Param("since") LocalDateTime since);

    // Videos with high engagement (views + likes)
    @Query("SELECT v FROM Video v WHERE v.privacy = :privacy " +
           "ORDER BY (COALESCE(v.viewCount, 0) + COALESCE(v.likeCount, 0) * 10) DESC")
    List<Video> findHighEngagementVideos(@Param("privacy") Video.Privacy privacy);

    // Count methods for statistics
    long countByPrivacy(Video.Privacy privacy);
    long countByCategoryIdAndPrivacy(Long categoryId, Video.Privacy privacy);
    long countByUploadedByAndPrivacy(User uploadedBy, Video.Privacy privacy);

    // Advanced search with multiple filters
    @Query("SELECT v FROM Video v WHERE v.privacy = :privacy AND " +
           "(:categoryId IS NULL OR v.category.id = :categoryId) AND " +
           "(:minDuration IS NULL OR v.duration >= :minDuration) AND " +
           "(:maxDuration IS NULL OR v.duration <= :maxDuration) AND " +
           "(:minLikes IS NULL OR v.likeCount >= :minLikes) AND " +
           "(:minViews IS NULL OR v.viewCount >= :minViews) AND " +
           "(:uploadedAfter IS NULL OR v.createdAt >= :uploadedAfter) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(v.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Video> findAdvancedFilteredVideos(
        @Param("privacy") Video.Privacy privacy,
        @Param("categoryId") Long categoryId,
        @Param("minDuration") Integer minDuration,
        @Param("maxDuration") Integer maxDuration,
        @Param("minLikes") Long minLikes,
        @Param("minViews") Long minViews,
        @Param("uploadedAfter") LocalDateTime uploadedAfter,
        @Param("search") String search);
}

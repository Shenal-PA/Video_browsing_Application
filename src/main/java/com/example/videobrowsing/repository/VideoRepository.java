package com.example.videobrowsing.repository;


import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository

public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video>findByUploader(User user);
    List<Video>findByCategory(Category category);
    List<Video>findByStatus(Video.Status status);
    List<Video>findByPrivacy(Video.Privacy privacy);

    @Query("SELECT v FROM Video v WHERE v.status='PUBLISHED'AND V.privacy='PUBLIC'ORDER BY V.createdAt DESC")
    List<Video>findPublicVideos();

    @Query("SELECT v FROM Video v WHERE v.status='PUBLISHED'AND v.privacy='PUBLIC'ORDER BY v.viewCount DESC")
    List<Video>findPublicVideosOder();

    @Query("SELECT v FROM Video v WHERE v.status='PUBLISHED' AND v.privacy='PUBLIC'ORDER BY v.createdAt DESC LIMIT 10")
    List<Video>findLatestPublicVideos();

    @Query("SELECT v FROM Video v WHERE v.status='PUBLISHED' AND v.privacy='PUBLIC' AND (v.title LIKE %:keyword% OR v.description LIKE %:keyword% OR v.tags LIKE %:keyword%)")
    List<Video>searchVideos(@Param("Keyword")String Keyword);

    @Query("SELECT v FROM Video v WHERE v.status = 'PUBLISHED' AND v.privacy = 'PUBLIC' AND v.category.id = :categoryId")
    List<Video> findRelatedVideos(@Param("category") Category category, @Param("videoId") Long videoId);

    @Query("SELECT COUNT(v) FROM Video v WHERE v.status = :status")
    Long countByStatus(@Param("status") Video.Status status);





}

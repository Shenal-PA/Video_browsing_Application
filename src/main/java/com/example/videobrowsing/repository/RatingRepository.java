package com.example.videobrowsing.repository;

import com.example.videobrowsing.entity.Rating;
import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByVideoAndUser(Video video, User user);
    List<Rating> findByVideo(Video video);
    List<Rating> findByUser(User user);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.video = :video AND r.ratingType = :ratingType")
    Long countByVideoAndRatingType(@Param("video") Video video, @Param("ratingType") Rating.RatingType ratingType);

    void deleteByVideoAndUser(Video video, User user);
}
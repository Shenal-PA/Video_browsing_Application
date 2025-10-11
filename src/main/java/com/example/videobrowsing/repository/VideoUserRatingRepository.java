package com.example.videobrowsing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.videobrowsing.entity.VideoUserRating;

@Repository
public interface VideoUserRatingRepository extends JpaRepository<VideoUserRating, Long> {

    Optional<VideoUserRating> findByVideoIdAndUserId(Long videoId, Long userId);

    long countByVideoId(Long videoId);

    @Query("SELECT AVG(r.score) FROM VideoUserRating r WHERE r.video.id = :videoId")
    Double findAverageScoreByVideoId(Long videoId);
}

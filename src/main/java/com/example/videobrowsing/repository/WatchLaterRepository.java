package com.example.videobrowsing.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.entity.WatchLater;

@Repository
public interface WatchLaterRepository extends JpaRepository<WatchLater, Long> {
    List<WatchLater> findByUser(User user);
    Optional<WatchLater> findByUserAndVideo(User user, Video video);
    boolean existsByUserAndVideo(User user, Video video);

    @Query("SELECT wl FROM WatchLater wl WHERE wl.user = :user ORDER BY wl.addedAt DESC")
    List<WatchLater> findByUserOrderByAddedAt(@Param("user") User user);

    void deleteByUserAndVideo(User user, Video video);
    void deleteByUser(User user);
}
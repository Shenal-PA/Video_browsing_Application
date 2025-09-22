package com.example.videobrowsing.repository;

import com.example.videobrowsing.entity.Playlist;
import com.example.videobrowsing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByUser(User user);
    List<Playlist> findByPrivacy(Playlist.Privacy privacy);

    @Query("SELECT p FROM Playlist p WHERE p.user = :user ORDER BY p.updatedAt DESC")
    List<Playlist> findByUserOrderByUpdatedAt(@Param("user") User user);

    @Query("SELECT p FROM Playlist p WHERE p.privacy = 'PUBLIC' AND (p.name LIKE %:keyword% OR p.description LIKE %:keyword%)")
    List<Playlist> searchPublicPlaylists(@Param("keyword") String keyword);
}
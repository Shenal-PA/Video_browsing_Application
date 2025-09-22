package com.example.videobrowsing.repository;

import com.example.videobrowsing.entity.PlaylistVideo;
import com.example.videobrowsing.entity.Playlist;
import com.example.videobrowsing.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlaylistVideoRepository extends JpaRepository<PlaylistVideo, Long> {
    List<PlaylistVideo> findByPlaylist(Playlist playlist);
    List<PlaylistVideo> findByVideo(Video video);
    Optional<PlaylistVideo> findByPlaylistAndVideo(Playlist playlist, Video video);

    @Query("SELECT pv FROM PlaylistVideo pv WHERE pv.playlist = :playlist ORDER BY pv.position ASC")
    List<PlaylistVideo> findByPlaylistOrderByPosition(@Param("playlist") Playlist playlist);

    @Query("SELECT MAX(pv.position) FROM PlaylistVideo pv WHERE pv.playlist = :playlist")
    Integer findMaxPositionInPlaylist(@Param("playlist") Playlist playlist);

    void deleteByPlaylistAndVideo(Playlist playlist, Video video);
}
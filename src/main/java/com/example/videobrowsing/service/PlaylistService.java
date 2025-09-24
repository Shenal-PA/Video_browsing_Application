package com.example.videobrowsing.service;


import com.example.videobrowsing.entity.Playlist;
import com.example.videobrowsing.entity.PlaylistVideo;
import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.entity.WatchLater;
import com.example.videobrowsing.dto.PlaylistDTO;
import com.example.videobrowsing.repository.PlaylistRepository;
import com.example.videobrowsing.repository.PlaylistVideoRepository;
import com.example.videobrowsing.repository.VideoRepository;
import com.example.videobrowsing.repository.WatchLaterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PlaylistService {

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private PlaylistVideoRepository playlistVideoRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private WatchLaterRepository watchLaterRepository;

    public Playlist createPlaylist(PlaylistDTO playlistDTO, User user) {
        Playlist playlist = new Playlist();
        playlist.setName(playlistDTO.getName());
        playlist.setDescription(playlistDTO.getDescription());
        playlist.setUser(user);

        if (playlistDTO.getPrivacy() != null) {
            playlist.setPrivacy(Playlist.Privacy.valueOf(playlistDTO.getPrivacy()));
        }

        if (playlistDTO.getIsCollaborative() != null) {
            playlist.setIsCollaborative(playlistDTO.getIsCollaborative());
        }

        return playlistRepository.save(playlist);
    }

    public List<Playlist> getUserPlaylists(User user) {
        return playlistRepository.findByUserOrderByUpdatedAt(user);
    }

    public List<Playlist> getPublicPlaylists() {
        return playlistRepository.findByPrivacy(Playlist.Privacy.PUBLIC);
    }

    public Optional<Playlist> getPlaylistById(Long id) {
        return playlistRepository.findById(id);
    }

    public Playlist updatePlaylist(Long playlistId, PlaylistDTO playlistDTO, User user) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        // Check if user owns the playlist
        if (!playlist.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to update this playlist");
        }

        if (playlistDTO.getName() != null) playlist.setName(playlistDTO.getName());
        if (playlistDTO.getDescription() != null) playlist.setDescription(playlistDTO.getDescription());
        if (playlistDTO.getPrivacy() != null) playlist.setPrivacy(Playlist.Privacy.valueOf(playlistDTO.getPrivacy()));
        if (playlistDTO.getIsCollaborative() != null) playlist.setIsCollaborative(playlistDTO.getIsCollaborative());

        return playlistRepository.save(playlist);
    }

    public void deletePlaylist(Long playlistId, User user) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        // Check if user owns the playlist or is admin
        if (!playlist.getUser().getId().equals(user.getId()) && !user.getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Not authorized to delete this playlist");
        }

        playlistRepository.delete(playlist);
    }

    public PlaylistVideo addVideoToPlaylist(Long playlistId, Long videoId, User user) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        // Check if user owns the playlist
        if (!playlist.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to modify this playlist");
        }

        // Check if video is already in playlist
        Optional<PlaylistVideo> existing = playlistVideoRepository.findByPlaylistAndVideo(playlist, video);
        if (existing.isPresent()) {
            throw new RuntimeException("Video already in playlist");
        }

        // Get next position
        Integer maxPosition = playlistVideoRepository.findMaxPositionInPlaylist(playlist);
        int nextPosition = (maxPosition != null) ? maxPosition + 1 : 1;

        PlaylistVideo playlistVideo = new PlaylistVideo();
        playlistVideo.setPlaylist(playlist);
        playlistVideo.setVideo(video);
        playlistVideo.setPosition(nextPosition);

        return playlistVideoRepository.save(playlistVideo);
    }

    public void removeVideoFromPlaylist(Long playlistId, Long videoId, User user) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        // Check if user owns the playlist
        if (!playlist.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to modify this playlist");
        }

        playlistVideoRepository.deleteByPlaylistAndVideo(playlist, video);
    }

    public List<PlaylistVideo> getPlaylistVideos(Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));
        return playlistVideoRepository.findByPlaylistOrderByPosition(playlist);
    }

    public List<Playlist> searchPublicPlaylists(String keyword) {
        return playlistRepository.searchPublicPlaylists(keyword);
    }

    // Watch Later functionality
    public WatchLater addToWatchLater(Long videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        // Check if already in watch later
        Optional<WatchLater> existing = watchLaterRepository.findByUserAndVideo(user, video);
        if (existing.isPresent()) {
            throw new RuntimeException("Video already in watch later");
        }

        WatchLater watchLater = new WatchLater();
        watchLater.setUser(user);
        watchLater.setVideo(video);

        return watchLaterRepository.save(watchLater);
    }

    public void removeFromWatchLater(Long videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        watchLaterRepository.deleteByUserAndVideo(user, video);
    }

    public List<WatchLater> getUserWatchLater(User user) {
        return watchLaterRepository.findByUserOrderByAddedAt(user);
    }
}
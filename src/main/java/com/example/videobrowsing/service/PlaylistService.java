package com.example.videobrowsing.service;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.videobrowsing.dto.PlaylistDTO;
import com.example.videobrowsing.dto.PlaylistVideoItemDTO;
import com.example.videobrowsing.entity.Playlist;
import com.example.videobrowsing.entity.PlaylistVideo;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.repository.PlaylistRepository;
import com.example.videobrowsing.repository.PlaylistVideoRepository;
import com.example.videobrowsing.repository.VideoRepository;

@Service
@Transactional
public class PlaylistService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private PlaylistVideoRepository playlistVideoRepository;

    @Autowired
    private VideoRepository videoRepository;


    public Playlist createPlaylist(PlaylistDTO playlistDTO, User user) {
        Playlist playlist = new Playlist();
        playlist.setName(playlistDTO.getName());
        playlist.setDescription(playlistDTO.getDescription());
        playlist.setUser(user);

        playlist.setPrivacy(Playlist.Privacy.PRIVATE);
        playlist.setIsCollaborative(Boolean.FALSE);
        playlist.setCreatedAt(LocalDateTime.now());
        playlist.setUpdatedAt(LocalDateTime.now());

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

        playlist.setPrivacy(Playlist.Privacy.PRIVATE);
        playlist.setIsCollaborative(Boolean.FALSE);
        playlist.setUpdatedAt(LocalDateTime.now());

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

    @Transactional(readOnly = true)
    public List<PlaylistDTO> getPlaylistSummaries(User user) {
        List<Playlist> playlists = playlistRepository.findByUserOrderByUpdatedAt(user);
        return playlists.stream()
                .map(playlist -> mapToDto(playlist, user, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlaylistDTO getPlaylistDetails(Long playlistId, User currentUser) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));
        return mapToDto(playlist, currentUser, true);
    }

    private PlaylistDTO mapToDto(Playlist playlist, User currentUser, boolean includeVideos) {
        PlaylistDTO dto = new PlaylistDTO();
        dto.setId(playlist.getId());
        dto.setName(playlist.getName());
        dto.setDescription(playlist.getDescription());
        dto.setOwnerId(playlist.getUser() != null ? playlist.getUser().getId() : null);
        dto.setOwnerDisplayName(resolveOwnerDisplayName(playlist.getUser()));
        dto.setCreatedAt(formatDate(playlist.getCreatedAt()));
        dto.setUpdatedAt(formatDate(playlist.getUpdatedAt()));
        dto.setPrivacy(Playlist.Privacy.PRIVATE.name());
        dto.setIsCollaborative(Boolean.FALSE);

        long count = playlistVideoRepository.countByPlaylist(playlist);
        dto.setVideoCount((int) count);

        boolean canEdit = currentUser != null && playlist.getUser() != null
                && playlist.getUser().getId().equals(currentUser.getId());
        dto.setCanEdit(canEdit);

        if (includeVideos) {
            List<PlaylistVideo> items = playlistVideoRepository.findByPlaylistOrderByPosition(playlist);
            if (items.isEmpty()) {
                dto.setVideos(Collections.emptyList());
            } else {
                List<PlaylistVideoItemDTO> videos = items.stream()
                        .map(this::mapPlaylistVideo)
                        .collect(Collectors.toList());
                dto.setVideos(videos);
                dto.setVideoCount(videos.size());
            }
        }

        return dto;
    }

    private PlaylistVideoItemDTO mapPlaylistVideo(PlaylistVideo playlistVideo) {
        PlaylistVideoItemDTO dto = new PlaylistVideoItemDTO();
        Video video = playlistVideo.getVideo();
        dto.setVideoId(video.getId());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        dto.setThumbnailPath(resolveThumbnailPath(video));
        dto.setDuration(video.getDuration());
        dto.setViewCount(video.getViewCount());
        dto.setLikeCount(video.getLikeCount());
        dto.setUploaderName(video.getUploadedBy() != null ? video.getUploadedBy().getUsername() : null);
        dto.setPosition(playlistVideo.getPosition());
        dto.setAddedAt(formatDate(playlistVideo.getAddedAt()));
        return dto;
    }

    private String resolveThumbnailPath(Video video) {
        if (video.getThumbnail() == null || video.getThumbnail().isBlank()) {
            return null;
        }
        return "/uploads/thumbnails/" + video.getThumbnail();
    }

    private String resolveOwnerDisplayName(User owner) {
        if (owner == null) {
            return "";
        }
        String first = owner.getFirstName();
        String last = owner.getLastName();
        if (first != null && !first.isBlank()) {
            if (last != null && !last.isBlank()) {
                return (first + " " + last).trim();
            }
            return first.trim();
        }
        if (owner.getUsername() != null) {
            return owner.getUsername();
        }
        return owner.getEmail();
    }

    private String formatDate(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(ISO_FORMATTER);
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

        PlaylistVideo saved = playlistVideoRepository.save(playlistVideo);
        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);
        return saved;
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

        List<PlaylistVideo> remaining = playlistVideoRepository.findByPlaylistOrderByPosition(playlist);
        int position = 1;
        for (PlaylistVideo item : remaining) {
            item.setPosition(position++);
        }
        if (!remaining.isEmpty()) {
            playlistVideoRepository.saveAll(remaining);
        }

        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);
    }

    public List<PlaylistVideo> getPlaylistVideos(Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));
        return playlistVideoRepository.findByPlaylistOrderByPosition(playlist);
    }

    public List<Playlist> searchPublicPlaylists(String keyword) {
        return playlistRepository.searchPublicPlaylists(keyword);
    }

    public PlaylistDTO reorderPlaylistVideos(Long playlistId, List<Long> orderedVideoIds, User user) {
        if (orderedVideoIds == null) {
            throw new RuntimeException("Video order is required");
        }

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        if (!playlist.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to modify this playlist");
        }

        List<PlaylistVideo> currentVideos = playlistVideoRepository.findByPlaylistOrderByPosition(playlist);
        if (currentVideos.isEmpty()) {
            return mapToDto(playlist, user, true);
        }

        Map<Long, PlaylistVideo> byVideoId = currentVideos.stream()
                .collect(Collectors.toMap(pv -> pv.getVideo().getId(), pv -> pv, (left, right) -> left, LinkedHashMap::new));

        int position = 1;
        for (Long videoId : orderedVideoIds) {
            PlaylistVideo item = byVideoId.remove(videoId);
            if (item != null) {
                item.setPosition(position++);
            }
        }

        if (!byVideoId.isEmpty()) {
            for (PlaylistVideo remaining : byVideoId.values()) {
                remaining.setPosition(position++);
            }
        }

        playlistVideoRepository.saveAll(currentVideos);
        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);

        return mapToDto(playlist, user, true);
    }

}
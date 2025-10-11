package com.example.videobrowsing.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.videobrowsing.dto.VideoDTO;
import com.example.videobrowsing.dto.WatchLaterDTO;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.entity.WatchLater;
import com.example.videobrowsing.repository.VideoRepository;
import com.example.videobrowsing.repository.WatchLaterRepository;

@Service
@Transactional
public class WatchLaterService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private WatchLaterRepository watchLaterRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private VideoService videoService;

    public WatchLaterDTO addToWatchLater(Long videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        if (watchLaterRepository.existsByUserAndVideo(user, video)) {
            throw new IllegalStateException("Video already in watch later");
        }

        WatchLater watchLater = new WatchLater(user, video);
        WatchLater saved = watchLaterRepository.save(watchLater);
        return mapToDto(saved, Optional.ofNullable(user));
    }

    public void removeFromWatchLater(Long videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        watchLaterRepository.deleteByUserAndVideo(user, video);
    }

    public void clearWatchLater(User user) {
        watchLaterRepository.deleteByUser(user);
    }

    public List<WatchLaterDTO> getWatchLater(User user) {
        List<WatchLater> items = watchLaterRepository.findByUserOrderByAddedAt(user);
        return items.stream()
                .map(item -> mapToDto(item, Optional.ofNullable(user)))
                .collect(Collectors.toList());
    }

    private WatchLaterDTO mapToDto(WatchLater watchLater, Optional<User> currentUser) {
        VideoDTO videoDTO = videoService.toDto(watchLater.getVideo(), currentUser);
        String addedAt = watchLater.getAddedAt() != null ? watchLater.getAddedAt().format(ISO_FORMATTER) : null;
        return new WatchLaterDTO(watchLater.getId(), addedAt, videoDTO);
    }
}

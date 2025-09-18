package com.example.videobrowsing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name="playlist_videos")

public class PlaylistVideo {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="playlist_id")
    @NotNull
    private Playlist playlist;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="video_id")
    @NotNull
    private Video video;

    @NotNull
    private Integer position;

    private LocalDateTime addedAt=LocalDateTime.now();

    public PlaylistVideo() {
    }
    public PlaylistVideo(Playlist playlist, Video video, Integer position) {
        this.playlist = playlist;
        this.video = video;
        this.position = position;

    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Playlist getPlaylist() {
        return playlist;
    }
    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }
    public Video getVideo() {
        return video;
    }
    public void setVideo(Video video) {
        this.video = video;
    }
    public Integer getPosition() {
        return position;
    }
    public void setPosition(Integer position) {
        this.position = position;
    }
    public LocalDateTime getAddedAt() {
        return addedAt;
    }
    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }



}

package com.example.videobrowsing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name="watch_later")

public class WatchLater {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @NotNull
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="video_id")
    @NotNull
    private Video video;

    private LocalDateTime addedAt=LocalDateTime.now();

    public WatchLater() {}

    public WatchLater(User user, Video video) {
        this.user = user;
        this.video = video;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public Video getVideo() {
        return video;
    }
    public void setVideo(Video video) {
        this.video = video;
    }
    public LocalDateTime getAddedAt() {
        return addedAt;
    }
    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }























}

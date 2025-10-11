package com.example.videobrowsing.dto;

public class WatchLaterDTO {

    private Long id;
    private String addedAt;
    private VideoDTO video;

    public WatchLaterDTO() {
    }

    public WatchLaterDTO(Long id, String addedAt, VideoDTO video) {
        this.id = id;
        this.addedAt = addedAt;
        this.video = video;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(String addedAt) {
        this.addedAt = addedAt;
    }

    public VideoDTO getVideo() {
        return video;
    }

    public void setVideo(VideoDTO video) {
        this.video = video;
    }
}

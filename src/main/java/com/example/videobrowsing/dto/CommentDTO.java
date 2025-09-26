package com.example.videobrowsing.dto;


import jakarta.validation.constraints.NotBlank;

public class CommentDTO {
    private Long id;
    private Long videoId;
    private Long userId;
    private String username;
    private String userProfilePicture;

    @NotBlank
    private String content;

    private Boolean isPinned;
    private Boolean isSpam;
    private Boolean isDisabled;
    private Long parentCommentId;
    private String createdAt;

    // Constructors
    public CommentDTO() {}

    public CommentDTO(Long videoId, Long userId, String content) {
        this.videoId = videoId;
        this.userId = userId;
        this.content = content;
    }

    // Getters and Setters
    public Long getId() { return id; }

    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getUserProfilePicture() { return userProfilePicture; }
    public void setUserProfilePicture(String userProfilePicture) { this.userProfilePicture = userProfilePicture; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Boolean getIsPinned() { return isPinned; }
    public void setIsPinned(Boolean isPinned) { this.isPinned = isPinned; }

    public Boolean getIsSpam() { return isSpam; }
    public void setIsSpam(Boolean isSpam) { this.isSpam = isSpam; }

    public Boolean getIsDisabled() { return isDisabled; }
    public void setIsDisabled(Boolean isDisabled) { this.isDisabled = isDisabled; }

    public Long getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(Long parentCommentId) { this.parentCommentId = parentCommentId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
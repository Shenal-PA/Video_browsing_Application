package com.example.videobrowsing.dto;


import jakarta.validation.constraints.NotBlank;

public class PlaylistDTO {
    private Long id;

    @NotBlank
    private String name;

    private String description;
    private Long userId;
    private String username;
    private String privacy;
    private Boolean isCollaborative;
    private Integer videoCount;
    private Long ownerId;
    private String ownerDisplayName;
    private String createdAt;
    private String updatedAt;
    private boolean canEdit;
    private java.util.List<PlaylistVideoItemDTO> videos;

    // Constructors
    public PlaylistDTO() {}

    public PlaylistDTO(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPrivacy() { return privacy; }
    public void setPrivacy(String privacy) { this.privacy = privacy; }

    public Boolean getIsCollaborative() { return isCollaborative; }
    public void setIsCollaborative(Boolean isCollaborative) { this.isCollaborative = isCollaborative; }

    public Integer getVideoCount() { return videoCount; }
    public void setVideoCount(Integer videoCount) { this.videoCount = videoCount; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getOwnerDisplayName() { return ownerDisplayName; }
    public void setOwnerDisplayName(String ownerDisplayName) { this.ownerDisplayName = ownerDisplayName; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public boolean isCanEdit() { return canEdit; }
    public void setCanEdit(boolean canEdit) { this.canEdit = canEdit; }

    public java.util.List<PlaylistVideoItemDTO> getVideos() { return videos; }
    public void setVideos(java.util.List<PlaylistVideoItemDTO> videos) { this.videos = videos; }
}
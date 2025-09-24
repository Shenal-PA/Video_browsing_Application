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
}
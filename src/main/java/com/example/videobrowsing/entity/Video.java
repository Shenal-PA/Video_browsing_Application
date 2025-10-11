package com.example.videobrowsing.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name="videos")
public class Video {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    @Column(columnDefinition="TEXT")
    private String description;

    @NotBlank
    @Column(name = "file_path")
    private String filepath;

    @Column(name = "thumbnail_path")
    private String thumbnail;
    private Integer duration;
    private Long fileSize;
    private String resolution;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="category_id")
    private Category category;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="uploaded_by", nullable = false)
    @NotNull
    private User uploadedBy;

    @Enumerated(EnumType.STRING)
    private Privacy privacy = Privacy.PUBLIC;

    @Enumerated(EnumType.STRING)
    private Status status=Status.PROCESSING;

    private Long viewCount=0L;
    private Long likeCount=0L;
    private Long dislikeCount=0L;

    @Column(columnDefinition="JSON")
    private String tags;

    private LocalDateTime createdAt=LocalDateTime.now();
    private LocalDateTime updatedAt=LocalDateTime.now();

    @OneToMany(mappedBy="video",cascade=CascadeType.ALL)
    @JsonIgnore
    private List<Comment>comments;

    @OneToMany(mappedBy ="video",cascade=CascadeType.ALL)
    @JsonIgnore
    private List<Rating>ratings;

    @OneToMany(mappedBy="video",cascade=CascadeType.ALL)
    @JsonIgnore
    private List<PlaylistVideo>playlistVideos;

    public enum Privacy{
        PUBLIC,PRIVATE
    }
    public enum Status{
        PUBLISHED,DISABLED,PROCESSING
    }
    public Video(){}
    public Video(String title,String description,String filepath,User uploadedBy){
        this.description=description;
        this.filepath=filepath;
        this.uploadedBy=uploadedBy;
        this.title=title;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getFilepath() {
        return filepath;
    }
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
    public String getFilePath() {
        return filepath;
    }
    public void setFilePath(String filePath) {
        this.filepath = filePath;
    }

    public String getThumbnail() {
        return thumbnail;
    }
    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getThumbnailPath() {
        return thumbnail;
    }
    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnail = thumbnailPath;
    }

    public Integer getDuration() {
        return duration;
    }
    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Long getFileSize() {
        return fileSize;
    }
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getResolution() {
        return resolution;
    }
    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public Category getCategory() {
        return category;
    }
    public void setCategory(Category category) {
        this.category = category;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }
    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Privacy getPrivacy() {
        return privacy;
    }
    public void setPrivacy(Privacy privacy) {
        this.privacy = privacy;
    }

    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getViewCount() {
        return viewCount;
    }
    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Long getLikeCount() {
        return likeCount;
    }
    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    public Long getDislikeCount() {
        return dislikeCount;
    }
    public void setDislikeCount(Long dislikeCount) {
        this.dislikeCount = dislikeCount;
    }

    public String getTags() {
        return tags;
    }
    public void setTags(String tags) {
        this.tags = tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Comment> getComments() {
        return comments;
    }
    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<Rating> getRatings() {
        return ratings;
    }
    public void setRatings(List<Rating> ratings) {
        this.ratings = ratings;
    }

    public List<PlaylistVideo> getPlaylistVideos() {
        return playlistVideos;
    }
    public void setPlaylistVideos(List<PlaylistVideo> playlistVideos) {
        this.playlistVideos = playlistVideos;
    }
}
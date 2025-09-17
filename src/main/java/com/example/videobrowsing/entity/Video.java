package com.example.videobrowsing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.xml.stream.events.Comment;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name="videos")

public class Video {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition="TEXT")
    private String description;

    @NotBlank
    private String filepath;

    private String thumbnail;
    private Integer duration;
    private Long fileSize;
    private String resolution;

    @ManyToOne(ferch=FetchType.LAZY)
    @JoinColumn(name="category_id")
    private Category category;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="uploaded_by")
    @NotNull
    private User uploadedBy;

    @Enumerated(EnumType.STRING)
    private privacy privacy=privacy.PUBLIC;

    @Enumerated(EnumType.STRING)
    private Status status=Status.PROCESSING;

    public enum Status{
        PUBLIC,PRICATE
    }
    public enum Status{
        PUBLICSHED,DISABLED,PROCESSING
    }
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
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String gettitle() {
        return title;
    }
    public void settitle(String title) {
        this.title=title;
    }
    public String getdescription() {
        return description;
    }
    public void setdescription(String description) {
        this.description = description;
    }
    public String getfilepath() {
        return filepath;
    }
    public void setfilepath(String filepath) {
        this.filepath = filepath;
    }
    public String getthumbnail() {
        return thumbnail;
    }
    public void setthumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }
    public Integer getduration() {
        return duration;
    }
    public void setduration(Integer duration) {
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
    public Category getCategory(){
        return category;
    }
    public void setCategory(Category category){
        this.category=category;
    }
    public User getUploadedBy(){
        return uploadedBy;
    }
    public void setUploadedBy(User uploadedBy){
        this.uploadedBy=uploadedBy;
    }
    public Privacy getPrivacy(){
        return privacy;
    }
    public void setPrivacy(Privacy privacy){
        this.privacy=privacy;
    }
    public Status getStatus(){
        return status;
    }
    public void setStatus(Status status){
        this.status=status;
    }
    public Long getViewCount(){
        return viewCount;
    }
    public void setViewCount(Long viewCount){
        this.viewCount=viewCount;
    }
    public Long getLikeCount(){
        return likeCount;
    }
    public void setLikeCount(Long likeCount){
        this.likeCount=likeCount;
    }
    public Long getDislikeCount(){
        return dislikeCount;
    }
    public void setDislikeCount(Long dislikeCount){
        this.dislikeCount=dislikeCount;
    }
    public String getTags(){
        return tags;
    }
    public void setTags(String tags){
        this.tags=tags;
    }
    public LocalDateTime getCreatedAt() {
        return  createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public LocalDateTime getUpdatedAt() {
        return  updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    public List<Comment>getComments(){
        return comments;
    }
    public void setComments(List<Comment> comments){
        this.comments=comments;
    }
    public List<Rating>getRatings(){
        return ratings;
    }
    public void setRatings(List<Rating> ratings){
        this.ratings=ratings;
    }
    public List<PlaylistVideo>getPlaylistVideos(){
        return playlistVideos;
    }
    public void setPlaylistVideos(List<PlaylistVideo> playlistVideos){
        this.playlistVideos=playlistVideos;
    }


}

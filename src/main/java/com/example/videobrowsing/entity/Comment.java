package com.example.videobrowsing.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name="comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="video_id")
    @NotNull
    private Video video;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id")
    @NotNull
    private User user;

    @NotBlank
    @Column(columnDefinition="TEXT")
    private String content;

    private Boolean isPinned=false;
    private Boolean isSpam=false;
    private Boolean isDisabled=false;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="parent_comment_id")
    private Comment parentComment;

    @OneToMany(mappedBy="parentComment",cascade=CascadeType.ALL)
    @JsonIgnore
    private List<Comment>replies;

    @OneToMany(mappedBy="comment", cascade=CascadeType.ALL, orphanRemoval=true)
    @JsonIgnore
    private List<CommentLike> likes = new ArrayList<>();

    private LocalDateTime createdAt=LocalDateTime.now();
    private LocalDateTime updatedAt=LocalDateTime.now();

    public Comment() {}
    public Comment(Video video, User user, String content) {
        this.video = video;
        this.user = user;
        this.content = content;
    }

    public Long getId(){
        return id;
    }
    public void setId(Long id){
        this.id=id;
    }
    public Video getVideo(){
        return video;
    }
    public void setVideo(Video video){
        this.video=video;
    }
    public User getUser(){
        return user;
    }
    public void setUser(User user){
        this.user=user;
    }
    public String getContent(){
        return content;
    }
    public void setContent(String content){
        this.content=content;
    }
    public Boolean getPinned(){
        return isPinned;
    }
    public void setPinned(Boolean pinned){
        isPinned=pinned;
    }
    public Boolean getSpam(){
        return isSpam;
    }
    public void setSpam(Boolean spam){
        this.isSpam=spam;
    }
    public Boolean getIsDisabled(){
        return isDisabled;
    }
    public void setIsDisabled(Boolean isDisabled){
        this.isDisabled=isDisabled;
    }
    public Comment getParentComment(){
        return parentComment;
    }
    public void setParentComment(Comment parentComment){
        this.parentComment=parentComment;
    }
    public List<Comment> getReplies(){
        return replies;
    }
    public void setReplies(List<Comment> replies){
        this.replies=replies;
    }
    public List<CommentLike> getLikes() {
        return likes;
    }
    public void setLikes(List<CommentLike> likes) {
        this.likes = likes;
    }
    public LocalDateTime getCreatedAt(){
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt=createdAt;
    }
    public LocalDateTime getUpdatedAt(){
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt){
        this.updatedAt=updatedAt;
    }































}

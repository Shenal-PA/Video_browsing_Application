package com.example.videobrowsing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name="ratings")
public class Rating {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="video_id")
    @NotNull
    private Video video;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id")
    @NotNull
    private User user;

    @Enumerated(EnumType.STRING)
    @NotNull
    private RatingType ratingType;

    private LocalDateTime createdAt=LocalDateTime.now();
    public enum RatingType{
        LIKE,DISLIKE
    }

    public Rating(){}

    public Rating(Video video,User user,RatingType ratingTyepe){
        this.video=video;
        this.user=user;
        this.ratingType=ratingTyepe;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Video getVideo() {
        return video;
    }
    public void setVideo(Video video) {
        this.video = video;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public RatingType getRatingType() {
        return ratingType;
    }
    public void setRatingType(RatingType ratingType) {
        this.ratingType = ratingType;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}

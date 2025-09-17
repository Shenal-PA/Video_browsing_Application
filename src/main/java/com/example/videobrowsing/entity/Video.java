package com.example.videobrowsing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonIgnore;
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



}

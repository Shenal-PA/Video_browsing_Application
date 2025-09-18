package com.example.videobrowsing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name="playlists")

public class Playlist {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @NotNull
    private User user;

    @Enumerated(EnumType.STRING)
    private Privacy privacy= Privacy.PRIVATE;

    private Boolean isCollaborative=false;

    private LocalDateTime createdAt=LocalDateTime.now();
    private LocalDateTime updatedAt=LocalDateTime.now();

    @OneToMany(mappedBy="playlist", cascade=CascadeType.ALL)
    private List<PlaylistVideo>playlistVideos;

    public enum Privacy{
        PRIVATE,PUBLIC
    }

    public Playlist() {}

    public Playlist(String name,String description,User user ){
        this.name=name;
        this.description=description;
        this.user=user;
    }
    public Long getId(){
        return id;
    }
    public void setId(Long id){
        this.id = id;
    }
    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }
    public String getDescription(){
        return description;
    }

    public void setDescription(String description){
        this.description = description;
    }
    public User getUser(){
        return user;
    }
    public void setUser(User user){
        this.user = user;
    }
    public Privacy getPrivacy(){
        return privacy;
    }
    public void setPrivacy(Privacy privacy){
        this.privacy = privacy;
    }
    public Boolean getIsCollaborative(){
        return isCollaborative;
    }
    public void setIsCollaborative(Boolean isCollaborative) {
        this.isCollaborative = isCollaborative;
    }
    public LocalDateTime getCreatedAt(){
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public LocalDateTime getUpdatedAt(){
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    public List<PlaylistVideo> getPlaylistVideos(){
        return playlistVideos;
    }
    public void setPlaylistVideos(List<PlaylistVideo> playlistVideos){
        this.playlistVideos = playlistVideos;
    }







































}

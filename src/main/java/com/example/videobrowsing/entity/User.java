package com.example.videobrowsing.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name="users")

public class User {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    @Size(min=3,max=50)
    private String username;
    @NotBlank
    @Email
    private String email;
    @NotBlank
    @Size(min=6)
    @JsonIgnore
    private String password;

    private String phone;
    private String firstname;
    private String lastname;
    @Column(columnDefinition="TEXT")
    private String bio;
    private String profilePicture;

    @Enumerated(EnumType.STRING)
    private Role role=Role.REGISTERED_USER;
    public enum Role{
        ADMIN,CONTENT_CREATOR,REGISTERED_USER
    }
    private Boolean isActive=true;
    private Boolean emailVerified=false;
    private Boolean phoneVerified=false;
    private Boolean termsAgreed=false;

    @Column (columnDefinition="JSON")
    private String privacySettings;

    @Column (columnDefinition="JSON")
    private String notificationSettings;

    private LocalDateTime createdAt=LocalDateTime.now();
    private LocalDateTime updatedAt=LocalDateTime.now();

    @OneToMany (mappedBy ="uploadedBy",cascade=CascadeType.ALL)
    @JsonIgnore
    private List<Video>uploadedVideos;

    @OneToMany (mappedBy="user")
    @JsonIgnore
    private List<Comment>comments;

    @OneToMany(mappedBy="user",cascade=CascadeType.ALL)
    @JsonIgnore
    private List<Playlist>playlists;

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Subscription> subscribers;

    @OneToMany(mappedBy = "subscriber", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Subscription> subscriptions;

    @Transient
    private Long subscriberCount = 0L;

    @Transient
    private Long subscriptionCount = 0L;


    //Getter and setters
    public User(){}
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
    public Long getId(){
        return id;
    }
    public void setId(Long id){
        this.id=id;
    }
    public String getUsername(){
        return username;
    }
    public void setUsername(String username){
        this.username=username;
    }
    public  String getEmail(){
        return email;
    }
    public void setEmail(String email){
        this.email=email;
    }
    public String getPassword(){
        return password;
    }
    public void setPassword(String password){
        this.password=password;
    }
    public String getPhone(){
        return phone;
    }
    public void setPhone(String phone){
        this.phone=phone;
    }
    public String getFirstname(){
        return firstname;
    }
    public void setFirstname(String firstname){
        this.firstname=firstname;
    }
    public String getLastname(){
        return lastname;
    }
    public void setLastname(String lastname){
        this.lastname=lastname;
    }
    public String getBio(){
        return bio;
    }
    public void setBio(String bio){
        this.bio=bio;
    }
    public String getProfilePicture(){
        return profilePicture;
    }
    public void setProfilePicture(String profilePicture){
        this.profilePicture=profilePicture;
    }
    public Role getRole(){
        return role;
    }
    public void setRole(Role role){
        this.role=role;
    }
    public Boolean getIsActive(){
        return isActive;
    }
    public void setIsActive(Boolean isActive){
        this.isActive=isActive;
    }
    public Boolean getEmailVerified(){
        return emailVerified;
    }
    public void setEmailVerified(Boolean emailVerified){
        this.emailVerified=emailVerified;
    }
    public Boolean getPhoneVerified(){
        return phoneVerified;
    }
    public void setPhoneVerified(Boolean phoneVerified){
        this.phoneVerified=phoneVerified;
    }
    public Boolean getTermsAgreed(){
        return termsAgreed;
    }
    public void setTermsAgreed(Boolean termsAgreed){
        this.termsAgreed=termsAgreed;
    }
    public String getPrivacySettings() {
        return privacySettings;
    }
    public void setPrivacySettings(String privacySettings) {
        this.privacySettings = privacySettings;
    }
    public String getNotificationSettings() {
        return notificationSettings;
    }
    public void setNotificationSettings(String notificationSettings) {
        this.notificationSettings = notificationSettings;
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

    public List<Video> getUploadedVideos(){
        return uploadedVideos;
    }
    public void setUploadedVideos(List<Video> uploadedVideos){
        this.uploadedVideos=uploadedVideos;
    }
    public List<Comment> getComments(){
        return comments;
    }
    public void setComments(List<Comment> comments){
        this.comments=comments;
    }
    public List<Playlist> getPlaylists(){
        return playlists;
    }
    public void setPlaylists(List<Playlist> playlists){
        this.playlists=playlists;
    }
    public List<Subscription> getSubscribers() {
        return subscribers;
    }
    public void setSubscribers(List<Subscription> subscribers) {
        this.subscribers = subscribers;
    }
    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }
    public void setSubscriptions(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }
    public Long getSubscriberCount() {
        return subscriberCount;
    }
    public void setSubscriberCount(Long subscriberCount) {
        this.subscriberCount = subscriberCount;
    }
    public Long getSubscriptionCount() {
        return subscriptionCount;
    }
    public void setSubscriptionCount(Long subscriptionCount) {
        this.subscriptionCount = subscriptionCount;
    }
    public String getFirstName() {
        return firstname;
    }
    public void setFirstName(String firstName) {
        this.firstname = firstName;
    }
    public String getLastName() {
        return lastname;
    }
    public void setLastName(String lastName) {
        this.lastname = lastName;
    }















}

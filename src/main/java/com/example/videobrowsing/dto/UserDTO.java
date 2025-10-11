package com.example.videobrowsing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserDTO {
    private Long id;

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min=6)
    private  String password;

    private String phone;
    private String firstName;
    private String lastName;
    private String bio;
    private String profilePicture;
    private String role;
    private Boolean termsAgreed;
    private Long subscriberCount;
    private Long subscriptionCount;

    public UserDTO(){}

    public UserDTO(String username,String email,String password){
        this.username=username;
        this.email=email;
        this.password=password;
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}

    public String getUsername() {return username;}
    public void setUsername(String username) {this.username = username;}

    public String getEmail() {return email;}
    public void setEmail(String email) {this.email = email;}

    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}

    public String getPhone() {return phone;}
    public void setPhone(String phone) {this.phone = phone;}

    public String getFirstName() {return firstName;}
    public void setFirstName(String firstName) {this.firstName = firstName;}

    public String getLastName() {return lastName;}
    public void setLastName(String lastName) {this.lastName = lastName;}

    public String getBio() {return bio;}
    public void setBio(String bio) {this.bio = bio;}

    public String getProfilePicture() {return profilePicture;}
    public void setProfilePicture(String profilePicture) {this.profilePicture = profilePicture;}

    public String getRole() {return role;}
    public void setRole(String role) {this.role = role;}

    public Boolean getTermsAgreed() {return termsAgreed;}
    public void setTermsAgreed(Boolean termsAgreed) {this.termsAgreed = termsAgreed;}

    public Long getSubscriberCount() {return subscriberCount;}
    public void setSubscriberCount(Long subscriberCount) {this.subscriberCount = subscriberCount;}

    public Long getSubscriptionCount() {return subscriptionCount;}
    public void setSubscriptionCount(Long subscriptionCount) {this.subscriptionCount = subscriptionCount;}


}































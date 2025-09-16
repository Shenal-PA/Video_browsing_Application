package com.example.videobrowsing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.List;

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














}

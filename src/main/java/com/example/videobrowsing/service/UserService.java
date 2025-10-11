package com.example.videobrowsing.service;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.videobrowsing.dto.UserDTO;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.repository.SubscriptionRepository;
import com.example.videobrowsing.repository.UserRepository;

import jakarta.servlet.http.HttpSession;
@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    public User registerUser(UserDTO userDTO) {
        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new RuntimeException("Username already exists!");
        }
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setPhone(userDTO.getPhone());
        user.setBio(userDTO.getBio());
        user.setTermsAgreed(userDTO.getTermsAgreed());

        if (userDTO.getProfilePicture() != null && !userDTO.getProfilePicture().trim().isEmpty()) {
            user.setProfilePicture(userDTO.getProfilePicture());
        } else {
            user.setProfilePicture(generateDefaultAvatar(user));
        }

        if (userDTO.getRole() != null) {
            user.setRole(User.Role.valueOf(userDTO.getRole()));
        }

        User saved = userRepository.save(user);
        applySubscriptionMetrics(saved);
        return saved;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserById(Long id) {
        return findById(id);
    }

    public Optional<User> findByIdWithMetrics(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        userOpt.ifPresent(this::applySubscriptionMetrics);
        return userOpt;
    }

    public Optional<User> resolveCurrentUser(HttpSession session) {
        Long sessionUserId = extractUserIdFromSession(session);
        if (sessionUserId != null) {
            return findByIdWithMetrics(sessionUserId);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        String principal = authentication.getName();
        if (principal == null || "anonymousUser".equalsIgnoreCase(principal)) {
            return Optional.empty();
        }

        Optional<User> userOpt = userRepository.findByUsername(principal);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(principal);
        }

        userOpt.ifPresent(user -> {
            applySubscriptionMetrics(user);
            if (session != null) {
                session.setAttribute("userId", user.getId());
                session.setAttribute("username", user.getUsername());
                session.setAttribute("role", user.getRole() != null ? user.getRole().name() : null);
            }
        });

        return userOpt;
    }

    public User requireWithMetrics(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        applySubscriptionMetrics(user);
        return user;
    }

    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        users.forEach(this::applySubscriptionMetrics);
        return users;
    }

    public List<User> getActiveUsers() {
        List<User> users = userRepository.findByIsActiveTrue();
        users.forEach(this::applySubscriptionMetrics);
        return users;
    }

    public User updateUser(Long userId, UserDTO userDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (userDTO.getFirstName() != null) user.setFirstName(userDTO.getFirstName());
        if (userDTO.getLastName() != null) user.setLastName(userDTO.getLastName());
        if (userDTO.getEmail() != null) user.setEmail(userDTO.getEmail());
        if (userDTO.getPhone() != null) user.setPhone(userDTO.getPhone());
        if (userDTO.getBio() != null) user.setBio(userDTO.getBio());
        if (userDTO.getProfilePicture() != null) user.setProfilePicture(userDTO.getProfilePicture());

        if (user.getProfilePicture() == null || user.getProfilePicture().trim().isEmpty()) {
            user.setProfilePicture(generateDefaultAvatar(user));
        }

        User saved = userRepository.save(user);
        applySubscriptionMetrics(saved);
        return saved;
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);
    }

    public boolean validateLogin(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(username);
        }

        if (userOpt.isPresent() && userOpt.get().getIsActive()) {
            return passwordEncoder.matches(password, userOpt.get().getPassword());
        }
        return false;
    }

    public User upgradeToContentCreator(Long userId, Map<String, String> creatorDetails) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user profile is complete enough
        if (user.getProfilePicture() == null || user.getProfilePicture().isEmpty()) {
            user.setProfilePicture(generateDefaultAvatar(user));
        }

        if (user.getBio() == null || user.getBio().length() < 10) {
            throw new RuntimeException("Please add a detailed bio before becoming a creator");
        }

        // Upgrade user role
        user.setRole(User.Role.CONTENT_CREATOR);

        // Save creator details in profile
        String creatorProfile = "{" +
                "\"contentType\":\"" + creatorDetails.getOrDefault("contentType", "") + "\"," +
                "\"channelDescription\":\"" + creatorDetails.getOrDefault("channelDescription", "") + "\"," +
                "\"socialLinks\":\"" + creatorDetails.getOrDefault("socialLinks", "") + "\"," +
                "\"uploadFrequency\":\"" + creatorDetails.getOrDefault("uploadFrequency", "") + "\"," +
                "\"applicationDate\":\"" + java.time.LocalDateTime.now() + "\"" +
                "}";

    // Store creator details in notification settings for now
    user.setNotificationSettings(creatorProfile);

    User saved = userRepository.save(user);
    applySubscriptionMetrics(saved);
    return saved;
    }

    private String generateDefaultAvatar(User user) {
        // Generate red video icon for admin users
        if (user.getRole() == User.Role.ADMIN) {
            return "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='200' viewBox='0 0 200 200'%3E%3Ccircle cx='100' cy='100' r='95' fill='%23dc3545'/%3E%3Cpath d='M70 60 L70 140 L140 100 Z' fill='white'/%3E%3C/svg%3E";
        }
        
        String username = Optional.ofNullable(user.getUsername()).orElse("user");
        String identifier = username + "|" + Optional.ofNullable(user.getEmail()).orElse("videohub");
        String seed = URLEncoder.encode(identifier, StandardCharsets.UTF_8);
        return "https://api.dicebear.com/7.x/bottts/svg?seed=" + seed;
    }

    public List<User> searchUsers(String keyword) {
        List<User> users = userRepository.findByUsernameContainingIgnoreCase(keyword);
        users.forEach(this::applySubscriptionMetrics);
        return users;
    }

    public Long getUserCountByRole(User.Role role) {
        return userRepository.countByRole(role);
    }

    public User changeUserRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(User.Role.valueOf(newRole));
        User saved = userRepository.save(user);
        applySubscriptionMetrics(saved);
        return saved;
    }

    private void applySubscriptionMetrics(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        long subscriberCount = subscriptionRepository.countByCreatorId(user.getId());
        long subscriptionCount = subscriptionRepository.countBySubscriberId(user.getId());
        user.setSubscriberCount(subscriberCount);
        user.setSubscriptionCount(subscriptionCount);
    }

    private Long extractUserIdFromSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object attribute = session.getAttribute("userId");
        if (attribute instanceof Long id) {
            return id;
        }
        if (attribute instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public void deleteUserAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Delete all user data (cascading deletes will handle videos, comments, etc.)
        userRepository.delete(user);
    }
}
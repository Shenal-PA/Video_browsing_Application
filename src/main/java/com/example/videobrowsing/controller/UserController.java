package com.example.videobrowsing.controller;


import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.videobrowsing.dto.UserDTO;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.service.UserService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserDTO userDTO) {
        try {
            userService.registerUser(userDTO);
            return ResponseEntity.ok("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody UserDTO loginRequest, HttpSession session) {
        try {
            boolean isValid = userService.validateLogin(loginRequest.getUsername(), loginRequest.getPassword());
            if (isValid) {
                Optional<User> userOpt = userService.findByUsername(loginRequest.getUsername());
                if (userOpt.isEmpty()) {
                    userOpt = userService.findByEmail(loginRequest.getUsername());
                }

                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    session.setAttribute("userId", user.getId());
                    session.setAttribute("username", user.getUsername());
                    session.setAttribute("role", user.getRole().toString());
                    return ResponseEntity.ok("Login successful");
                }
            }
            return ResponseEntity.badRequest().body("Invalid credentials");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body("Not logged in");
        }

        Optional<User> user = userService.findByIdWithMetrics(userId);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.badRequest().body("User not found");
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.findByIdWithMetrics(id);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.badRequest().body("User not found");
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UserDTO userDTO, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body("Not logged in");
        }

        try {
            User updatedUser = userService.updateUser(userId, userDTO);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsers(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.badRequest().build();
        }

        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String keyword) {
        List<User> users = userService.searchUsers(keyword);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        Optional<User> userOpt = userService.resolveCurrentUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Not logged in");
        }

        User currentUser = userOpt.get();
        UserDTO userResponse = new UserDTO();
        userResponse.setId(currentUser.getId());
        userResponse.setUsername(currentUser.getUsername());
        userResponse.setFirstName(currentUser.getFirstname());
        userResponse.setLastName(currentUser.getLastname());
        userResponse.setEmail(currentUser.getEmail());
        userResponse.setPhone(currentUser.getPhone());
        userResponse.setBio(currentUser.getBio());
        userResponse.setProfilePicture(currentUser.getProfilePicture());
        userResponse.setRole(currentUser.getRole() != null ? currentUser.getRole().toString() : null);
        userResponse.setTermsAgreed(currentUser.getTermsAgreed());
        userResponse.setSubscriberCount(currentUser.getSubscriberCount());
        userResponse.setSubscriptionCount(currentUser.getSubscriptionCount());
        return ResponseEntity.ok(userResponse);
    }

    @PostMapping("/become-creator")
    public ResponseEntity<?> becomeCreator(@RequestBody Map<String, String> creatorDetails, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("Not logged in");
        }

        try {
            User updatedUser = userService.upgradeToContentCreator(userId, creatorDetails);
            // Update the session role
            session.setAttribute("role", updatedUser.getRole().toString());
            return ResponseEntity.ok("Successfully upgraded to content creator");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("Not logged in");
        }

        try {
            userService.deleteUserAccount(userId);
            session.invalidate();
            return ResponseEntity.ok("Account deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

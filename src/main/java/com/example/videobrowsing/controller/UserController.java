package com.example.videobrowsing.controller;


import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.dto.UserDTO;
import com.example.videobrowsing.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
        import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserDTO userDTO) {
        try {
            User user = userService.registerUser(userDTO);
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

        Optional<User> user = userService.findById(userId);
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

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.badRequest().body("Admin access required");
        }

        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok("User deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String keyword) {
        List<User> users = userService.searchUsers(keyword);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body("Not logged in");
        }

        Optional<User> user = userService.findById(userId);
        if (user.isPresent()) {
            UserDTO userDTO = new UserDTO();
            User u = user.get();
            userDTO.setId(u.getId());
            userDTO.setUsername(u.getUsername());
            userDTO.setEmail(u.getEmail());
            userDTO.setFirstName(u.getFirstName());
            userDTO.setLastName(u.getLastName());
            userDTO.setBio(u.getBio());
            userDTO.setProfilePicture(u.getProfilePicture());
            userDTO.setRole(u.getRole().toString());
            return ResponseEntity.ok(userDTO);
        }
        return ResponseEntity.badRequest().body("User not found");
    }
}

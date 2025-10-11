package com.example.videobrowsing.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.videobrowsing.dto.WatchLaterDTO;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.service.UserService;
import com.example.videobrowsing.service.WatchLaterService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/watch-later")
@CrossOrigin(origins = "*")
public class WatchLaterController {

    @Autowired
    private WatchLaterService watchLaterService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<?> getWatchLater(HttpSession session) {
        Optional<User> userOpt = resolveSessionUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        List<WatchLaterDTO> items = watchLaterService.getWatchLater(userOpt.get());
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{videoId}")
    public ResponseEntity<?> addToWatchLater(@PathVariable Long videoId, HttpSession session) {
        Optional<User> userOpt = resolveSessionUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        try {
            WatchLaterDTO dto = watchLaterService.addToWatchLater(videoId, userOpt.get());
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", ex.getMessage()
            ));
        } catch (RuntimeException ex) {
            HttpStatus status = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not found")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(ex.getMessage());
        }
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<?> removeFromWatchLater(@PathVariable Long videoId, HttpSession session) {
        Optional<User> userOpt = resolveSessionUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        try {
            watchLaterService.removeFromWatchLater(videoId, userOpt.get());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            HttpStatus status = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not found")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(ex.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> clearWatchLater(HttpSession session) {
        Optional<User> userOpt = resolveSessionUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        watchLaterService.clearWatchLater(userOpt.get());
        return ResponseEntity.noContent().build();
    }

    private Optional<User> resolveSessionUser(HttpSession session) {
        return userService.resolveCurrentUser(session);
    }
}

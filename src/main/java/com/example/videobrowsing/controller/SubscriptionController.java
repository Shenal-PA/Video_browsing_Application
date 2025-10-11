package com.example.videobrowsing.controller;

import java.util.HashMap;
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

import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.service.SubscriptionService;
import com.example.videobrowsing.service.UserService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "*")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserService userService;

    @PostMapping("/{creatorId}")
    public ResponseEntity<?> subscribe(@PathVariable Long creatorId, HttpSession session) {
        Optional<User> subscriberOpt = resolveSessionUser(session);
        if (subscriberOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        Optional<User> creatorOpt = subscriptionService.findUserById(creatorId);
        if (creatorOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Creator not found");
        }

        User subscriber = subscriberOpt.get();
        User creator = creatorOpt.get();

        try {
            subscriptionService.subscribe(subscriber, creator);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }

        return ResponseEntity.ok(buildSummaryResponse(subscriber, creator, true));
    }

    @DeleteMapping("/{creatorId}")
    public ResponseEntity<?> unsubscribe(@PathVariable Long creatorId, HttpSession session) {
        Optional<User> subscriberOpt = resolveSessionUser(session);
        if (subscriberOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        Optional<User> creatorOpt = subscriptionService.findUserById(creatorId);
        if (creatorOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Creator not found");
        }

        User subscriber = subscriberOpt.get();
        User creator = creatorOpt.get();

        try {
            subscriptionService.unsubscribe(subscriber, creator);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }

        return ResponseEntity.ok(buildSummaryResponse(subscriber, creator, false));
    }

    @GetMapping("/{creatorId}")
    public ResponseEntity<?> getSubscriptionStatus(@PathVariable Long creatorId, HttpSession session) {
        Optional<User> creatorOpt = subscriptionService.findUserById(creatorId);
        if (creatorOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Creator not found");
        }

        Optional<User> subscriberOpt = resolveSessionUser(session);
        User subscriber = subscriberOpt.orElse(null);
        User creator = creatorOpt.get();

        boolean subscribed = subscriber != null && !subscriber.getId().equals(creator.getId())
                && subscriptionService.isSubscribed(subscriber, creator);
        Map<String, Object> payload = new HashMap<>();
        payload.put("subscribed", subscribed);
        payload.put("subscriberCount", subscriptionService.countSubscribers(creator));
        payload.put("subscriptionCount", subscriber != null ? subscriptionService.countSubscriptions(subscriber) : null);
        payload.put("creatorId", creator.getId());
        return ResponseEntity.ok(payload);
    }

    private Map<String, Object> buildSummaryResponse(User subscriber, User creator, boolean subscribed) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("subscribed", subscribed);
        payload.put("subscriberCount", subscriptionService.countSubscribers(creator));
        payload.put("subscriptionCount", subscriptionService.countSubscriptions(subscriber));
        payload.put("creatorId", creator.getId());
        return payload;
    }

    private Optional<User> resolveSessionUser(HttpSession session) {
        return userService.resolveCurrentUser(session);
    }
}

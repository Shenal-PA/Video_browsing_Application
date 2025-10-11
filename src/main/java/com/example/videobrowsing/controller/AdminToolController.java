package com.example.videobrowsing.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin-tools")
public class AdminToolController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/encode-password")
    public ResponseEntity<?> encodePassword(@RequestParam String password) {
        String encoded = passwordEncoder.encode(password);
        return ResponseEntity.ok(encoded);
    }
}

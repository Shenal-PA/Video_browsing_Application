package com.example.videobrowsing.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.videobrowsing.dto.ReportDTO;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.service.ReportService;
import com.example.videobrowsing.service.UserService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> reportVideo(@RequestBody ReportDTO reportDTO, HttpSession session) {
        Optional<User> userOpt = resolveSessionUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        if (reportDTO.getVideoId() == null) {
            return ResponseEntity.badRequest().body("Video ID is required");
        }
        if (reportDTO.getReportType() == null || reportDTO.getReportType().isBlank()) {
            return ResponseEntity.badRequest().body("Report type is required");
        }

        try {
            reportService.createVideoReport(reportDTO, userOpt.get());
            return ResponseEntity.ok(Map.of("status", "submitted"));
        } catch (RuntimeException ex) {
            HttpStatus status = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not found")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(ex.getMessage());
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitReport(@RequestBody ReportDTO reportDTO, HttpSession session) {
        try {
            // Get current user from session (optional - reports can be anonymous)
            Optional<User> userOpt = resolveSessionUser(session);
            reportService.submitReport(reportDTO, userOpt.orElse(null));
            return ResponseEntity.ok(Map.of("message", "Report submitted successfully"));
        } catch (Exception e) {
            e.printStackTrace(); // Log the error for debugging
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to submit report: " + e.getMessage()));
        }
    }

    @GetMapping("/admin")
    public ResponseEntity<?> getAllReports() {
        try {
            return ResponseEntity.ok(reportService.getAllReports());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get reports: " + e.getMessage());
        }
    }

    @GetMapping("/my-reports")
    public ResponseEntity<?> getMyReports(HttpSession session) {
        try {
            Optional<User> userOpt = resolveSessionUser(session);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not logged in"));
            }
            
            return ResponseEntity.ok(reportService.getUserReports(userOpt.get()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get reports: " + e.getMessage()));
        }
    }

    @PutMapping("/admin/{id}/status")
    public ResponseEntity<?> updateReportStatus(@PathVariable Long id, @RequestParam String status, HttpSession session) {
        try {
            // Get admin user from session
            Optional<User> userOpt = resolveSessionUser(session);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not logged in"));
            }
            
            reportService.updateReportStatus(id, status, userOpt.get());
            return ResponseEntity.ok(Map.of("message", "Report status updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update report status: " + e.getMessage()));
        }
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<?> updateReport(@PathVariable Long id, @RequestBody Map<String, String> updates, HttpSession session) {
        try {
            // Get admin user from session
            Optional<User> userOpt = resolveSessionUser(session);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not logged in"));
            }
            
            User admin = userOpt.get();
            if (admin.getRole() == null || !admin.getRole().name().equals("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin access required"));
            }

            String status = updates.get("status");
            String adminNotes = updates.get("adminNotes");
            
            reportService.updateReportWithNotes(id, status, adminNotes, admin);
            return ResponseEntity.ok(Map.of("message", "Report updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update report: " + e.getMessage()));
        }
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> deleteReport(@PathVariable Long id, 
                                         @RequestBody(required = false) Map<String, String> request,
                                         HttpSession session) {
        try {
            // Verify admin privileges
            Optional<User> userOpt = resolveSessionUser(session);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not logged in"));
            }
            
            User user = userOpt.get();
            if (user.getRole() == null || !user.getRole().name().equals("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin access required"));
            }

            // Get deletion reason if provided
            String deletionReason = null;
            if (request != null && request.containsKey("deletionReason")) {
                deletionReason = request.get("deletionReason");
            }

            reportService.deleteReport(id, deletionReason);
            return ResponseEntity.ok(Map.of("message", "Report deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to delete report: " + e.getMessage()));
        }
    }

    private Optional<User> resolveSessionUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Optional.empty();
        }
        return userService.findById(userId);
    }
}

package com.example.videobrowsing.controller;



import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.entity.Report;
import com.example.videobrowsing.repository.UserRepository;
import com.example.videobrowsing.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMIN')") // Only ADMIN can access any endpoint in this controller
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportRepository reportRepository;

    // Promote a user to ADMIN
    @PostMapping("/promote")
    public ResponseEntity<?> promoteToAdmin(@RequestParam Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }
        user.setRole(User.Role.ADMIN);
        userRepository.save(user);
        return ResponseEntity.ok("User " + user.getUsername() + " promoted to ADMIN.");
    }

    // Demote an admin to a regular user
    @PostMapping("/demote")
    public ResponseEntity<?> demoteAdmin(@RequestParam Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }
        if (user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.badRequest().body("User is not an admin");
        }
        user.setRole(User.Role.REGISTERED_USER);
        userRepository.save(user);
        return ResponseEntity.ok("User " + user.getUsername() + " demoted to REGISTERED_USER.");
    }

    // List all users
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // List all reports
    @GetMapping("/reports")
    public ResponseEntity<List<Report>> getAllReports() {
        return ResponseEntity.ok(reportRepository.findAll());
    }

    // Resolve a report
    @PostMapping("/resolve-report")
    public ResponseEntity<?> resolveReport(@RequestParam Long reportId) {
        Report report = reportRepository.findById(reportId).orElse(null);
        if (report == null) {
            return ResponseEntity.badRequest().body("Report not found");
        }
        report.setStatus(Report.Status.RESOLVED);
        reportRepository.save(report);
        return ResponseEntity.ok("Report resolved successfully.");
    }
}
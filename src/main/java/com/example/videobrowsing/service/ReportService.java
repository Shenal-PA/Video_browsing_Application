package com.example.videobrowsing.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.videobrowsing.dto.ReportDTO;
import com.example.videobrowsing.entity.Comment;
import com.example.videobrowsing.entity.Report;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.repository.CommentRepository;
import com.example.videobrowsing.repository.ReportRepository;
import com.example.videobrowsing.repository.VideoRepository;

@Service
@Transactional
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private EmailService emailService;

    public Report createVideoReport(ReportDTO reportDTO, User user) {
        Video video = videoRepository.findById(reportDTO.getVideoId())
                .orElseThrow(() -> new RuntimeException("Video not found"));

        Report report = new Report();
        report.setVideo(video);
        report.setReportedBy(user);
        report.setReportType(Report.ReportType.valueOf(reportDTO.getReportType().toUpperCase()));
        report.setDescription(buildDescription(reportDTO.getVideoTitle(), reportDTO.getDescription(), reportDTO.getSubject()));
        report.setCreatedAt(LocalDateTime.now());
        report.setStatus(Report.Status.PENDING);

        return reportRepository.save(report);
    }

    public Report createCommentReport(ReportDTO reportDTO, User user) {
        Comment comment = commentRepository.findById(reportDTO.getCommentId())
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        Report report = new Report();
        report.setComment(comment);
        report.setReportedBy(user);
        report.setReportType(Report.ReportType.valueOf(reportDTO.getReportType().toUpperCase()));
        report.setDescription(reportDTO.getDescription());
        report.setCreatedAt(LocalDateTime.now());
        report.setStatus(Report.Status.PENDING);

        return reportRepository.save(report);
    }

    // New method for help center reports
    public Report submitReport(ReportDTO reportDTO, User reportedBy) {
        Report report = new Report();

        // Map form fields to report entity
        String reportType = reportDTO.getType() != null ? reportDTO.getType() : reportDTO.getReportType();

        try {
            report.setReportType(Report.ReportType.valueOf(reportType.toUpperCase()));
        } catch (IllegalArgumentException e) {
            report.setReportType(Report.ReportType.OTHER);
        }

        report.setDescription(buildDescription(reportDTO.getVideoTitle(), reportDTO.getDescription(), reportDTO.getSubject()));
        report.setCreatedAt(LocalDateTime.now());
        report.setStatus(Report.Status.PENDING);
        
        // Set the user who reported (can be null for anonymous reports)
        report.setReportedBy(reportedBy);
        
        // Save reporter's email from the form (for email notifications)
        if (reportDTO.getReporterEmail() != null && !reportDTO.getReporterEmail().trim().isEmpty()) {
            report.setReporterEmail(reportDTO.getReporterEmail().trim());
        }

        linkVideoIfPresent(reportDTO).ifPresent(report::setVideo);

        Report savedReport = reportRepository.save(report);
        
        // Send email notification to user
        try {
            emailService.sendReportSubmittedEmail(savedReport);
        } catch (Exception e) {
            // Log but don't fail if email fails
            System.err.println("Failed to send report submission email: " + e.getMessage());
        }
        
        return savedReport;
    }

    public List<ReportDTO> getAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ReportDTO> getUserReports(User user) {
        return reportRepository.findByReportedByOrderByCreatedAtDesc(user).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void updateReportStatus(Long reportId, String status, User admin) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        String oldStatus = report.getStatus().toString();
        report.setStatus(Report.Status.valueOf(status.toUpperCase()));

        if (status.equalsIgnoreCase("RESOLVED") || status.equalsIgnoreCase("DISMISSED")) {
            report.setResolvedAt(LocalDateTime.now());
            report.setResolvedBy(admin);
        }

        Report savedReport = reportRepository.save(report);
        
        // Send email notification to reporter
        try {
            emailService.sendReportStatusUpdateEmail(savedReport, oldStatus, status);
        } catch (Exception e) {
            System.err.println("Failed to send status update email: " + e.getMessage());
        }
    }

    public void updateReportWithNotes(Long reportId, String status, String adminNotes, User admin) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        String oldStatus = report.getStatus().toString();
        boolean statusChanged = false;

        if (status != null && !status.isBlank()) {
            report.setStatus(Report.Status.valueOf(status.toUpperCase()));
            statusChanged = true;
            
            if (status.equalsIgnoreCase("RESOLVED") || status.equalsIgnoreCase("DISMISSED")) {
                report.setResolvedAt(LocalDateTime.now());
                report.setResolvedBy(admin);
            }
        }

        if (adminNotes != null) {
            report.setAdminNotes(adminNotes);
        }

        Report savedReport = reportRepository.save(report);
        
        // Send email if status changed
        if (statusChanged) {
            try {
                emailService.sendReportStatusUpdateEmail(savedReport, oldStatus, status);
            } catch (Exception e) {
                System.err.println("Failed to send status update email: " + e.getMessage());
            }
        }
    }

    public void deleteReport(Long reportId) {
        deleteReport(reportId, null);
    }

    public void deleteReport(Long reportId, String deletionReason) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        
        // Mark report as DELETED instead of physically deleting it
        report.setStatus(Report.Status.DELETED);
        report.setDeletionReason(deletionReason);
        report.setResolvedAt(LocalDateTime.now());
        reportRepository.save(report);
        
        // Send email notification about deletion
        try {
            if (emailService != null) {
                emailService.sendReportDeletedEmail(report, deletionReason);
            }
        } catch (Exception e) {
            System.err.println("Failed to send deletion email: " + e.getMessage());
        }
    }

    private ReportDTO convertToDTO(Report report) {
        ReportDTO dto = new ReportDTO();
        dto.setId(report.getId());
        dto.setReportType(report.getReportType().toString());
        dto.setDescription(report.getDescription());
        dto.setStatus(report.getStatus().toString());
        dto.setCreatedAt(report.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Include admin notes and deletion reason for users to see
        dto.setAdminNotes(report.getAdminNotes());
        dto.setDeletionReason(report.getDeletionReason());

        if (report.getVideo() != null) {
            dto.setVideoId(report.getVideo().getId());
            dto.setVideoTitle(report.getVideo().getTitle());
        }

        if (report.getComment() != null) {
            dto.setCommentId(report.getComment().getId());
        }

        if (report.getReportedBy() != null) {
            dto.setReportedById(report.getReportedBy().getId());
            dto.setReportedByUsername(report.getReportedBy().getUsername());
            dto.setReportedByEmail(report.getReportedBy().getEmail());
        }

        if (report.getResolvedBy() != null) {
            dto.setResolvedById(report.getResolvedBy().getId());
            dto.setResolvedByUsername(report.getResolvedBy().getUsername());
        }

        if (report.getResolvedAt() != null) {
            dto.setResolvedAt(report.getResolvedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        return dto;
    }

    private Optional<Video> linkVideoIfPresent(ReportDTO reportDTO) {
        if (reportDTO.getVideoId() != null) {
            return videoRepository.findById(reportDTO.getVideoId());
        }

        if (reportDTO.getContentUrl() != null && reportDTO.getContentUrl().contains("/video")) {
            try {
                String[] parts = reportDTO.getContentUrl().split("/video");
                if (parts.length > 1) {
                    String trailing = parts[1];
                    String digits = trailing.replaceAll("[^0-9]", "");
                    if (!digits.isEmpty()) {
                        Long videoId = Long.parseLong(digits);
                        return videoRepository.findById(videoId);
                    }
                }
            } catch (NumberFormatException ignored) {
                // ignore malformed id
            }
        }
        return Optional.empty();
    }

    private String buildDescription(String videoTitle, String description, String subject) {
        StringBuilder sb = new StringBuilder();

        if (subject != null && !subject.isBlank()) {
            sb.append(subject.trim());
        }

        if (videoTitle != null && !videoTitle.isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("Video: ").append(videoTitle.trim());
        }

        if (description != null && !description.isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(description.trim());
        }

        return sb.length() > 0 ? sb.toString() : null;
    }
}

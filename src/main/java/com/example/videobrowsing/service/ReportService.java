package com.example.videobrowsing.service;

import com.example.videobrowsing.entity.Report;
import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.entity.Comment;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.dto.ReportDTO;
import com.example.videobrowsing.repository.ReportRepository;
import com.example.videobrowsing.repository.VideoRepository;
import com.example.videobrowsing.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private CommentRepository commentRepository;

    public Report createVideoReport(ReportDTO reportDTO, User user) {
        Video video = videoRepository.findById(reportDTO.getVideoId())
                .orElseThrow(() -> new RuntimeException("Video not found"));

        Report report = new Report();
        report.setVideo(video);
        report.setReportedBy(user);
        report.setReportType(Report.ReportType.valueOf(reportDTO.getReportType()));
        report.setDescription(reportDTO.getDescription());

        return reportRepository.save(report);
    }

    public Report createCommentReport(ReportDTO reportDTO, User user) {
        Comment comment = commentRepository.findById(reportDTO.getCommentId())
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        Report report = new Report();
        report.setComment(comment);
        report.setReportedBy(user);
        report.setReportType(Report.ReportType.valueOf(reportDTO.getReportType()));
        report.setDescription(reportDTO.getDescription());

        return reportRepository.save(report);
    }

    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    public List<Report> getPendingReports() {
        return reportRepository.findPendingReports();
    }

    public List<Report> getReportsByStatus(String status) {
        return reportRepository.findByStatus(Report.Status.valueOf(status));
    }

    public List<Report> getReportsByType(String reportType) {
        return reportRepository.findByReportType(Report.ReportType.valueOf(reportType));
    }

    public Optional<Report> getReportById(Long id) {
        return reportRepository.findById(id);
    }

    public Report updateReportStatus(Long reportId, String status, String adminNotes, User admin) {
        if (!admin.getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Only admins can update report status");
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setStatus(Report.Status.valueOf(status));
        report.setAdminNotes(adminNotes);
        report.setResolvedBy(admin);

        if (status.equals("RESOLVED") || status.equals("DISMISSED")) {
            report.setResolvedAt(LocalDateTime.now());
        }

        return reportRepository.save(report);
    }

    public void deleteReport(Long reportId, User admin) {
        if (!admin.getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("Only admins can delete reports");
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        reportRepository.delete(report);
    }

    public List<Report> getUserReports(User user) {
        return reportRepository.findByReportedBy(user);
    }

    public List<Report> getVideoReports(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        return reportRepository.findByVideo(video);
    }

    public Long getReportCountByStatus(String status) {
        return reportRepository.countByStatus(Report.Status.valueOf(status));
    }

    public List<Report> getReportsByAdmin(User admin) {
        return reportRepository.findReportsByAdmin(admin);
    }
}

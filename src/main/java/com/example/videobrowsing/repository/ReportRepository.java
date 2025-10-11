package com.example.videobrowsing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.videobrowsing.entity.Report;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.entity.Video;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    // Basic finder methods
    List<Report> findByReportedBy(User user);
    List<Report> findByReportedByOrderByCreatedAtDesc(User user);
    List<Report> findByVideo(Video video);
    List<Report> findByStatus(Report.Status status);
    List<Report> findByReportType(Report.ReportType reportType);

    // Method to get all reports ordered by creation date (newest first)
    @Query("SELECT r FROM Report r ORDER BY r.createdAt DESC")
    List<Report> findAllByOrderByCreatedAtDesc();

    // Custom queries
    @Query("SELECT r FROM Report r WHERE r.status = 'PENDING' ORDER BY r.createdAt ASC")
    List<Report> findPendingReports();

    @Query("SELECT COUNT(r) FROM Report r WHERE r.status = :status")
    Long countByStatus(@Param("status") Report.Status status);

    @Query("SELECT r FROM Report r WHERE r.resolvedBy = :admin ORDER BY r.resolvedAt DESC")
    List<Report> findReportsByAdmin(@Param("admin") User admin);
}
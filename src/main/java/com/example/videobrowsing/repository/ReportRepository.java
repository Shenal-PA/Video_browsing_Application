package com.example.videobrowsing.repository;

import com.example.videobrowsing.entity.Report;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByReportedBy(User user);
    List<Report> findByVideo(Video video);
    List<Report> findByStatus(Report.Status status);
    List<Report> findByReportType(Report.ReportType reportType);

    @Query("SELECT r FROM Report r WHERE r.status = 'PENDING' ORDER BY r.createdAt ASC")
    List<Report> findPendingReports();

    @Query("SELECT COUNT(r) FROM Report r WHERE r.status = :status")
    Long countByStatus(@Param("status") Report.Status status);

    @Query("SELECT r FROM Report r WHERE r.resolvedBy = :admin ORDER BY r.resolvedAt DESC")
    List<Report> findReportsByAdmin(@Param("admin") User admin);
}
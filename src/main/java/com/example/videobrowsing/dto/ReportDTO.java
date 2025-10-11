package com.example.videobrowsing.dto;

public class ReportDTO {
    private Long id;
    private Long videoId;
    private String videoTitle;
    private Long commentId;
    private Long reportedById;
    private String reportedByUsername;
    private String reportedByEmail;

    private String reportType;

    // New fields for help center form
    private String type; // Maps to reportType for form compatibility
    private String contentUrl;
    private String subject;
    private String reporterEmail;

    private String description;
    private String status;
    private String adminNotes;
    private String deletionReason;  // Reason provided when report is deleted
    private Long resolvedById;
    private String resolvedByUsername;
    private String createdAt;
    private String resolvedAt;

    // Constructors
    public ReportDTO() {}

    public ReportDTO(Long videoId, Long reportedById, String reportType, String description) {
        this.videoId = videoId;
        this.reportedById = reportedById;
        this.reportType = reportType;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }

    public String getVideoTitle() { return videoTitle; }
    public void setVideoTitle(String videoTitle) { this.videoTitle = videoTitle; }

    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }

    public Long getReportedById() { return reportedById; }
    public void setReportedById(Long reportedById) { this.reportedById = reportedById; }

    public String getReportedByUsername() { return reportedByUsername; }
    public void setReportedByUsername(String reportedByUsername) { this.reportedByUsername = reportedByUsername; }

    public String getReportedByEmail() { return reportedByEmail; }
    public void setReportedByEmail(String reportedByEmail) { this.reportedByEmail = reportedByEmail; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getType() { return type; }
    public void setType(String type) {
        this.type = type;
        this.reportType = type; // Sync with reportType
    }

    public String getContentUrl() { return contentUrl; }
    public void setContentUrl(String contentUrl) { this.contentUrl = contentUrl; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getReporterEmail() { return reporterEmail; }
    public void setReporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public String getDeletionReason() { return deletionReason; }
    public void setDeletionReason(String deletionReason) { this.deletionReason = deletionReason; }

    public Long getResolvedById() { return resolvedById; }
    public void setResolvedById(Long resolvedById) { this.resolvedById = resolvedById; }

    public String getResolvedByUsername() { return resolvedByUsername; }
    public void setResolvedByUsername(String resolvedByUsername) { this.resolvedByUsername = resolvedByUsername; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }
}
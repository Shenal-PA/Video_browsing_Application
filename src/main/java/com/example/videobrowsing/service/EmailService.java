package com.example.videobrowsing.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.videobrowsing.entity.Report;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@videobrowsing.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Send email notification when report status is updated
     */
    public void sendReportStatusUpdateEmail(Report report, String oldStatus, String newStatus) {
        if (mailSender == null) {
            // Email not configured
            return;
        }

        // Get email address - prefer reporter's email from form, fallback to user email
        String recipientEmail = getReporterEmail(report);
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            return; // No email available
        }

        // Get reporter name
        String reporterName = getReporterName(report);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipientEmail);
            message.setSubject("Report Update - Your Report #" + report.getId());
            
            StringBuilder body = new StringBuilder();
            body.append("Hello ").append(reporterName).append(",\n\n");
            body.append("Your report (#").append(report.getId()).append(") status has been updated.\n\n");
            body.append("Previous Status: ").append(oldStatus).append("\n");
            body.append("Current Status: ").append(newStatus).append("\n\n");
            
            body.append("Report Details:\n");
            body.append("- Type: ").append(report.getReportType().toString().replace("_", " ")).append("\n");
            body.append("- Submitted: ").append(report.getCreatedAt()).append("\n");
            
            if (report.getVideo() != null) {
                body.append("- Related Video: ").append(report.getVideo().getTitle()).append("\n");
            }
            
            if (report.getAdminNotes() != null && !report.getAdminNotes().trim().isEmpty()) {
                body.append("\nAdmin Notes:\n");
                body.append(report.getAdminNotes()).append("\n");
            }
            
            if (newStatus.equals("RESOLVED") || newStatus.equals("DISMISSED")) {
                body.append("\nThis report has been closed. ");
                if (report.getResolvedBy() != null) {
                    body.append("Handled by: Admin").append("\n");
                }
            }
            
            body.append("\nYou can view your report history in your profile: ");
            body.append(baseUrl).append("/profile\n\n");
            body.append("Thank you for helping us maintain a safe community!\n\n");
            body.append("Best regards,\n");
            body.append("VideoHub Support Team");
            
            message.setText(body.toString());
            
            mailSender.send(message);
        } catch (Exception e) {
            // Log error but don't fail the transaction
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    /**
     * Send welcome notification when report is first submitted
     */
    public void sendReportSubmittedEmail(Report report) {
        if (mailSender == null) {
            return;
        }

        // Get email address - prefer reporter's email from form, fallback to user email
        String recipientEmail = getReporterEmail(report);
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            return; // No email available
        }

        // Get reporter name
        String reporterName = getReporterName(report);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipientEmail);
            message.setSubject("Report Received - Report #" + report.getId());
            
            StringBuilder body = new StringBuilder();
            body.append("Hello ").append(reporterName).append(",\n\n");
            body.append("We have received your report (#").append(report.getId()).append(").\n\n");
            body.append("Report Details:\n");
            body.append("- Type: ").append(report.getReportType().toString().replace("_", " ")).append("\n");
            body.append("- Status: PENDING\n");
            body.append("- Submitted: ").append(report.getCreatedAt()).append("\n");
            
            if (report.getVideo() != null) {
                body.append("- Related Video: ").append(report.getVideo().getTitle()).append("\n");
            }
            
            body.append("\nOur team will review your report and take appropriate action.\n");
            body.append("You will receive updates via email when the status changes.\n\n");
            body.append("You can track your report in your profile: ");
            body.append(baseUrl).append("/profile\n\n");
            body.append("Thank you for helping us maintain a safe community!\n\n");
            body.append("Best regards,\n");
            body.append("VideoHub Support Team");
            
            message.setText(body.toString());
            
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    /**
     * Send notification when report is deleted by admin
     */
    public void sendReportDeletedEmail(Report report, String deletionReason) {
        if (mailSender == null) {
            return;
        }

        // Get email address - prefer reporter's email from form, fallback to user email
        String recipientEmail = getReporterEmail(report);
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            return; // No email available
        }

        // Get reporter name
        String reporterName = getReporterName(report);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipientEmail);
            message.setSubject("Report Deleted - Report #" + report.getId());
            
            StringBuilder body = new StringBuilder();
            body.append("Hello ").append(reporterName).append(",\n\n");
            body.append("Your report (#").append(report.getId()).append(") has been removed by an administrator.\n\n");
            
            body.append("Report Details:\n");
            body.append("- Type: ").append(report.getReportType().toString().replace("_", " ")).append("\n");
            body.append("- Submitted: ").append(report.getCreatedAt()).append("\n");
            
            if (report.getVideo() != null) {
                body.append("- Related Video: ").append(report.getVideo().getTitle()).append("\n");
            }
            
            if (deletionReason != null && !deletionReason.trim().isEmpty()) {
                body.append("\n╔════════════════════════════════════════════════╗\n");
                body.append("║ Reason for Deletion:\n");
                body.append("║\n");
                body.append("║ ").append(deletionReason.replace("\n", "\n║ ")).append("\n");
                body.append("╚════════════════════════════════════════════════╝\n");
            } else {
                body.append("\nReason: No specific reason provided.\n");
            }
            
            body.append("\nThis report has been permanently removed from our system.\n");
            body.append("If you believe this was done in error, please contact our support team.\n\n");
            body.append("Thank you for your understanding.\n\n");
            body.append("Best regards,\n");
            body.append("VideoHub Support Team");
            
            message.setText(body.toString());
            
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send deletion email: " + e.getMessage());
        }
    }

    /**
     * Helper method to get reporter's email address
     * Priority: reporterEmail field from form > user's email > null
     */
    private String getReporterEmail(Report report) {
        // First, try to get email from report form (reporterEmail field)
        if (report.getReporterEmail() != null && !report.getReporterEmail().trim().isEmpty()) {
            return report.getReporterEmail().trim();
        }
        
        // Fallback to user's email if logged in
        if (report.getReportedBy() != null && report.getReportedBy().getEmail() != null) {
            return report.getReportedBy().getEmail();
        }
        
        return null; // No email available
    }

    /**
     * Helper method to get reporter's name for email greeting
     */
    private String getReporterName(Report report) {
        // If user is logged in, use their username
        if (report.getReportedBy() != null && report.getReportedBy().getUsername() != null) {
            return report.getReportedBy().getUsername();
        }
        
        // For anonymous reports, extract name from email or use "User"
        if (report.getReporterEmail() != null && !report.getReporterEmail().trim().isEmpty()) {
            String email = report.getReporterEmail();
            // Extract name before @ symbol
            int atIndex = email.indexOf('@');
            if (atIndex > 0) {
                return email.substring(0, atIndex);
            }
        }
        
        return "User"; // Default fallback
    }
}

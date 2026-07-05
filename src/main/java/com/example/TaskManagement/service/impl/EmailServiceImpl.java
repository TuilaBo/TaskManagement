package com.example.TaskManagement.service.impl;

import com.example.TaskManagement.dto.TaskUpdateEmailContext;
import com.example.TaskManagement.model.Task;
import com.example.TaskManagement.model.enums.TaskStatus;
import com.example.TaskManagement.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String FONT_FAMILY = "'Segoe UI', Arial, sans-serif";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendVerificationCodeEmail(String email, String code) {
        String subject = "[Task Management] Mã xác minh đăng ký tài khoản";
        String body = buildVerificationHtml(email, code);
        sendHtmlEmail(email, subject, body);
    }

    @Override
    public void sendTaskAssignmentEmail(Task task) {
        String subject = "[Task Management] Bạn được giao công việc mới: " + task.getTitle();
        String body = buildAssignmentHtml(task);
        sendHtmlEmail(task.getAssignedTo().getEmail(), subject, body);
    }

    @Override
    public void sendTaskReminderEmail(Task task) {
        String subject = "[Task Management] Nhắc nhở hoàn thành: " + task.getTitle();
        String body = buildReminderHtml(task);
        sendHtmlEmail(task.getAssignedTo().getEmail(), subject, body);
    }

    @Override
    public void sendTaskUpdatedEmail(Task task, TaskUpdateEmailContext context) {
        if (!context.hasChanges()) {
            return;
        }
        String subject = "[Task Management] Công việc được chỉnh sửa: " + task.getTitle();
        String body = buildUpdateHtml(task, context);
        sendHtmlEmail(task.getAssignedTo().getEmail(), subject, body);
    }

    // ============================================================
    // HTML TEMPLATES
    // ============================================================

    private String buildVerificationHtml(String email, String code) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0;padding:0;background-color:#f4f6f9;font-family:%s">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f9;padding:40px 20px">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0"
                                       style="background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08)">
                                    <!-- Header -->
                                    <tr>
                                        <td style="background:linear-gradient(135deg,#4a90d9,#357abd);padding:36px 40px;text-align:center">
                                            <div style="font-size:32px;margin-bottom:8px">&#128273;</div>
                                            <h1 style="color:#ffffff;font-size:22px;font-weight:600;margin:0">
                                                Xác minh tài khoản
                                            </h1>
                                            <p style="color:rgba(255,255,255,0.85);font-size:14px;margin:10px 0 0">
                                                Task Management System
                                            </p>
                                        </td>
                                    </tr>
                                    <!-- Body -->
                                    <tr>
                                        <td style="padding:36px 40px">
                                            <p style="font-size:15px;color:#374151;margin:0 0 24px">
                                                Xin chào,
                                            </p>
                                            <p style="font-size:15px;color:#374151;margin:0 0 24px">
                                                Mã xác minh đăng ký tài khoản Staff của bạn là:
                                            </p>
                                            <!-- Code Box -->
                                            <div style="background:#f8fafc;border:2px dashed #4a90d9;border-radius:10px;
                                                        padding:24px;text-align:center;margin-bottom:24px">
                                                <div style="font-size:36px;font-weight:700;color:#4a90d9;
                                                            letter-spacing:12px;font-family:'Courier New',monospace">
                                                    %s
                                                </div>
                                            </div>
                                            <!-- Warning -->
                                            <div style="background:#fff7ed;border-left:4px solid #f59e0b;
                                                        border-radius:0 8px 8px 0;padding:14px 18px;margin-bottom:24px">
                                                <p style="font-size:14px;color:#92400e;margin:0">
                                                    &#9888; Mã có hiệu lực trong <strong>10 phút</strong>.
                                                    Vui lòng không chia sẻ mã này với bất kỳ ai.
                                                </p>
                                            </div>
                                            <p style="font-size:13px;color:#6b7280;margin:0">
                                                Nếu bạn không thực hiện đăng ký, vui lòng bỏ qua email này.
                                            </p>
                                        </td>
                                    </tr>
                                    <!-- Footer -->
                                    <tr>
                                        <td style="background:#f9fafb;padding:20px 40px;text-align:center;
                                                    border-top:1px solid #e5e7eb">
                                            <p style="font-size:12px;color:#9ca3af;margin:0">
                                                &#169; 2026 Task Management System
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(FONT_FAMILY, code);
    }

    private String buildAssignmentHtml(Task task) {
        boolean isByCategory = task.getCategory() != null;
        String categoryName = isByCategory ? task.getCategory().getName() : "";
        String categoryTag = isByCategory
                ? "<div style=\"display:inline-block;background:#eff6ff;color:#1d4ed8;"
                  + "border-radius:20px;padding:4px 14px;font-size:12px;font-weight:600;margin-top:4px\">"
                  + "&#128193; " + escapeHtml(categoryName) + "</div>"
                : "";

        String categorySection = isByCategory
                ? "<td style=\"padding:8px 0;border-bottom:1px solid #f3f4f6\">"
                  + "<span style=\"font-size:13px;color:#6b7280;display:block;margin-bottom:2px\">"
                  + "&#128193; Danh mục</span>"
                  + "<span style=\"font-size:15px;color:#1f2937;font-weight:500\">"
                  + escapeHtml(categoryName) + "</span></td>"
                : "";

        return buildAssignmentHtmlCore(
                task.getAssignedTo().getUsername(),
                task.getCreatedBy().getUsername(),
                task.getTitle(),
                task.getDescription() != null ? task.getDescription() : "Không có mô tả chi tiết",
                formatDateTime(task.getStartDate()),
                formatDateTime(task.getDeadline()),
                categoryTag,
                categorySection
        );
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private String buildAssignmentHtmlCore(
            String assignee, String creator, String title,
            String description, String startDate, String deadline,
            String categoryTag, String categorySection) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0;padding:0;background-color:#f4f6f9;font-family:%s">
                    <table width="100%%" cellpadding="0" cellspacing="0"
                           style="background-color:#f4f6f9;padding:40px 20px">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0"
                                       style="background-color:#ffffff;border-radius:12px;
                                              overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08)">
                                    <!-- Header -->
                                    <tr>
                                        <td style="background:linear-gradient(135deg,#16a34a,#15803d);
                                                    padding:32px 40px">
                                            <div style="display:flex;align-items:center;gap:12px">
                                                <div style="background:rgba(255,255,255,0.2);border-radius:50%%;
                                                            width:48px;height:48px;text-align:center;line-height:48px;
                                                            font-size:22px">
                                                    &#128203;
                                                </div>
                                                <div>
                                                    <h1 style="color:#ffffff;font-size:20px;font-weight:600;margin:0">
                                                        Bạn được giao công việc mới
                                                    </h1>
                                                    <p style="color:rgba(255,255,255,0.85);font-size:13px;margin:6px 0 0">
                                                        Từ %s &bull; %s
                                                    </p>
                                                </div>
                                            </div>
                                        </td>
                                    </tr>
                                    <!-- Title Banner -->
                                    <tr>
                                        <td style="padding:24px 40px 0">
                                            <div style="background:#f0fdf4;border-radius:10px;
                                                        padding:20px;border-left:4px solid #16a34a">
                                                <h2 style="color:#14532d;font-size:18px;font-weight:700;margin:0 0 8px">
                                                    %s
                                                </h2>
                                                %s
                                            </div>
                                        </td>
                                    </tr>
                                    <!-- Detail Table -->
                                    <tr>
                                        <td style="padding:16px 40px 0">
                                            <table width="100%%" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td style="padding:8px 0;border-bottom:1px solid #f3f4f6">
                                                        <span style="font-size:13px;color:#6b7280;display:block;margin-bottom:2px">
                                                            &#128197; Ngày bắt đầu
                                                        </span>
                                                        <span style="font-size:15px;color:#1f2937;font-weight:500">%s</span>
                                                    </td>
                                                    <td style="padding:8px 0;border-bottom:1px solid #f3f4f6">
                                                        <span style="font-size:13px;color:#6b7280;display:block;margin-bottom:2px">
                                                            &#9200; Hạn hoàn thành
                                                        </span>
                                                        <span style="font-size:15px;color:#dc2626;font-weight:600">%s</span>
                                                    </td>
                                                </tr>
                                                %s
                                            </table>
                                        </td>
                                    </tr>
                                    <!-- Description -->
                                    <tr>
                                        <td style="padding:16px 40px 0">
                                            <span style="font-size:13px;color:#6b7280;display:block;margin-bottom:6px">
                                                &#128221; Mô tả công việc
                                            </span>
                                            <div style="background:#f9fafb;border-radius:8px;padding:16px">
                                                <p style="font-size:14px;color:#374151;margin:0;line-height:1.6">
                                                    %s
                                                </p>
                                            </div>
                                        </td>
                                    </tr>
                                    <!-- CTA -->
                                    <tr>
                                        <td style="padding:28px 40px 32px">
                                            <div style="text-align:center">
                                                <a href="#" style="display:inline-block;
                                                           background:linear-gradient(135deg,#16a34a,#15803d);
                                                           color:#ffffff;text-decoration:none;border-radius:8px;
                                                           padding:14px 32px;font-size:15px;font-weight:600">
                                                    &#128073; Xem chi tiết trên hệ thống
                                                </a>
                                            </div>
                                        </td>
                                    </tr>
                                    <!-- Footer -->
                                    <tr>
                                        <td style="background:#f9fafb;padding:20px 40px;text-align:center;
                                                    border-top:1px solid #e5e7eb">
                                            <p style="font-size:12px;color:#9ca3af;margin:0">
                                                &#169; 2026 Task Management System
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                FONT_FAMILY,
                creator,
                java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                title,
                categoryTag,
                startDate,
                deadline,
                categorySection,
                description
        );
    }

    private String buildReminderHtml(Task task) {
        boolean isOverdue = task.getDeadline() != null
                && task.getDeadline().isBefore(java.time.LocalDateTime.now());

        String urgencyColor = isOverdue ? "#dc2626" : "#d97706";
        String urgencyBg = isOverdue ? "#fef2f2" : "#fffbeb";
        String urgencyBorder = isOverdue ? "#fca5a5" : "#fcd34d";
        String urgencyIcon = isOverdue ? "&#9888;" : "&#128276;";
        String urgencyTitle = isOverdue ? "Quá hạn!" : "Sắp đến hạn";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0;padding:0;background-color:#f4f6f9;font-family:%s">
                    <table width="100%%" cellpadding="0" cellspacing="0"
                           style="background-color:#f4f6f9;padding:40px 20px">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0"
                                       style="background-color:#ffffff;border-radius:12px;
                                              overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08)">
                                    <!-- Header -->
                                    <tr>
                                        <td style="background:linear-gradient(135deg,#d97706,#b45309);
                                                    padding:32px 40px">
                                            <div style="display:flex;align-items:center;gap:12px">
                                                <div style="background:rgba(255,255,255,0.2);border-radius:50%%;
                                                            width:48px;height:48px;text-align:center;line-height:48px;
                                                            font-size:22px">
                                                    %s
                                                </div>
                                                <div>
                                                    <h1 style="color:#ffffff;font-size:20px;font-weight:600;margin:0">
                                                        Nhắc nhở hoàn thành công việc
                                                    </h1>
                                                    <p style="color:rgba(255,255,255,0.85);font-size:13px;margin:6px 0 0">
                                                        Task Management System
                                                    </p>
                                                </div>
                                            </div>
                                        </td>
                                    </tr>
                                    <!-- Urgency Banner -->
                                    <tr>
                                        <td style="padding:20px 40px 0">
                                            <div style="background:%s;border:1px solid %s;
                                                        border-radius:8px;padding:12px 16px;text-align:center">
                                                <span style="font-size:13px;color:%s;font-weight:600">
                                                    %s %s
                                                </span>
                                            </div>
                                        </td>
                                    </tr>
                                    <!-- Task Info -->
                                    <tr>
                                        <td style="padding:20px 40px 0">
                                            <div style="background:#f9fafb;border-radius:10px;padding:20px">
                                                <h2 style="color:#111827;font-size:17px;font-weight:700;margin:0 0 12px">
                                                    %s
                                                </h2>
                                                <table width="100%%" cellpadding="0" cellspacing="0">
                                                    <tr>
                                                        <td style="padding:4px 0">
                                                            <span style="font-size:13px;color:#6b7280">&#128337; Hạn hoàn thành:</span>
                                                            <span style="font-size:14px;color:#1f2937;font-weight:600;margin-left:8px">%s</span>
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td style="padding:4px 0">
                                                            <span style="font-size:13px;color:#6b7280">&#128308; Trạng thái:</span>
                                                            <span style="font-size:14px;color:#d97706;font-weight:600;margin-left:8px">
                                                                %s
                                                            </span>
                                                        </td>
                                                    </tr>
                                                </table>
                                                %s
                                            </div>
                                        </td>
                                    </tr>
                                    <!-- CTA -->
                                    <tr>
                                        <td style="padding:24px 40px 32px">
                                            <div style="text-align:center">
                                                <a href="#" style="display:inline-block;
                                                           background:linear-gradient(135deg,#d97706,#b45309);
                                                           color:#ffffff;text-decoration:none;border-radius:8px;
                                                           padding:14px 32px;font-size:15px;font-weight:600">
                                                    &#127919; Hoàn thành ngay
                                                </a>
                                            </div>
                                        </td>
                                    </tr>
                                    <!-- Footer -->
                                    <tr>
                                        <td style="background:#f9fafb;padding:20px 40px;text-align:center;
                                                    border-top:1px solid #e5e7eb">
                                            <p style="font-size:12px;color:#9ca3af;margin:0">
                                                &#169; 2026 Task Management System
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                FONT_FAMILY,
                urgencyIcon,
                urgencyBg, urgencyBorder, urgencyColor, urgencyIcon, urgencyTitle,
                task.getTitle(),
                formatDateTime(task.getDeadline()),
                task.getStatus().name(),
                task.getDescription() != null
                        ? """
                          <p style="font-size:13px;color:#6b7280;margin:12px 0 0">&#128221; Mô tả:</p>
                          <p style="font-size:14px;color:#374151;margin:4px 0 0;line-height:1.5">""" + task.getDescription() + """
                          </p>
                          """
                        : ""
        );
    }

    private String buildUpdateHtml(Task task, TaskUpdateEmailContext context) {
        StringBuilder changesRows = new StringBuilder();
        for (String change : context.getChanges()) {
            changesRows.append("""
                    <tr>
                        <td style="padding:10px 16px;background:#fffbeb;
                                    border-radius:6px;margin-bottom:6px">
                            <span style="font-size:13px;color:#92400e">%s</span>
                        </td>
                    </tr>
                    """.formatted(change));
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0;padding:0;background-color:#f4f6f9;font-family:%s">
                    <table width="100%%" cellpadding="0" cellspacing="0"
                           style="background-color:#f4f6f9;padding:40px 20px">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0"
                                       style="background-color:#ffffff;border-radius:12px;
                                              overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08)">
                                    <!-- Header -->
                                    <tr>
                                        <td style="background:linear-gradient(135deg,#7c3aed,#6d28d9);
                                                    padding:32px 40px">
                                            <div style="display:flex;align-items:center;gap:12px">
                                                <div style="background:rgba(255,255,255,0.2);border-radius:50%%;
                                                            width:48px;height:48px;text-align:center;line-height:48px;
                                                            font-size:22px">
                                                    &#9998;
                                                </div>
                                                <div>
                                                    <h1 style="color:#ffffff;font-size:20px;font-weight:600;margin:0">
                                                        Công việc được chỉnh sửa
                                                    </h1>
                                                    <p style="color:rgba(255,255,255,0.85);font-size:13px;margin:6px 0 0">
                                                        Bởi %s &bull; %s
                                                    </p>
                                                </div>
                                            </div>
                                        </td>
                                    </tr>
                                    <!-- Task Title -->
                                    <tr>
                                        <td style="padding:24px 40px 0">
                                            <div style="background:#f5f3ff;border-radius:10px;
                                                        padding:20px;border-left:4px solid #7c3aed">
                                                <h2 style="color:#4c1d95;font-size:18px;font-weight:700;margin:0 0 6px">
                                                    %s
                                                </h2>
                                                <div style="display:flex;gap:16px;margin-top:10px">
                                                    <div>
                                                        <span style="font-size:12px;color:#7c3aed;font-weight:600">
                                                            &#128197; Bắt đầu
                                                        </span>
                                                        <div style="font-size:14px;color:#1f2937;font-weight:500">%s</div>
                                                    </div>
                                                    <div>
                                                        <span style="font-size:12px;color:#7c3aed;font-weight:600">
                                                            &#9200; Hạn chót
                                                        </span>
                                                        <div style="font-size:14px;color:#dc2626;font-weight:600">%s</div>
                                                    </div>
                                                </div>
                                            </div>
                                        </td>
                                    </tr>
                                    <!-- Changes -->
                                    <tr>
                                        <td style="padding:16px 40px 0">
                                            <span style="font-size:13px;color:#6b7280;font-weight:600;text-transform:uppercase;
                                                        letter-spacing:0.5px">
                                                Thay đổi
                                            </span>
                                            <div style="margin-top:10px">%s</div>
                                        </td>
                                    </tr>
                                    <!-- CTA -->
                                    <tr>
                                        <td style="padding:24px 40px 32px">
                                            <div style="text-align:center">
                                                <a href="#" style="display:inline-block;
                                                           background:linear-gradient(135deg,#7c3aed,#6d28d9);
                                                           color:#ffffff;text-decoration:none;border-radius:8px;
                                                           padding:14px 32px;font-size:15px;font-weight:600">
                                                    &#128073; Xem trên hệ thống
                                                </a>
                                            </div>
                                        </td>
                                    </tr>
                                    <!-- Footer -->
                                    <tr>
                                        <td style="background:#f9fafb;padding:20px 40px;text-align:center;
                                                    border-top:1px solid #e5e7eb">
                                            <p style="font-size:12px;color:#9ca3af;margin:0">
                                                &#169; 2026 Task Management System
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                FONT_FAMILY,
                context.getEditorUsername(),
                java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                task.getTitle(),
                formatDateTime(task.getStartDate()),
                formatDateTime(task.getDeadline()),
                changesRows.toString()
        );
    }

    // ============================================================
    // SEND
    // ============================================================

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMAT) : "Chưa có";
    }
}

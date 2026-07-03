package service;

import com.mongodb.client.MongoCollection;
import database.DatabaseHelper;
import javafx.application.Platform;
import org.bson.Document;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import jakarta.mail.*;
import jakarta.mail.internet.*;

public class EmailService {

    // ===== CẤU HÌNH GMAIL =====
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    private static final String SENDER_EMAIL = "vathe809@gmail.com"; // <-- đổi thành Gmail của bạn
    private static final String SENDER_PASS = "iqjs slke ijvl mwta"; // <-- App Password 16 ký tự

    // ---------------------------------------------------------------------------
    // FIX TC-ABS-003 / TC-ABS-004: Dedicated thread pool cho email
    // KHÔNG BAO GIỜ gọi sendEmail() trực tiếp trên JavaFX Application Thread.
    // Luôn sử dụng sendAbsenceAlertAsync() để tránh UI Thread Freeze.
    // ---------------------------------------------------------------------------
    private static final ExecutorService EMAIL_THREAD_POOL =
            Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "EmailService-Worker");
                t.setDaemon(true);
                return t;
            });

    private static final EmailService INSTANCE = new EmailService();

    public static EmailService getInstance() {
        return INSTANCE;
    }

    private EmailService() {
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Gửi OTP khôi phục mật khẩu (đồng bộ — gọi từ background thread của server).
     */
    public void sendOTP(String toEmail, String otp) throws Exception {
        sendEmailSync(toEmail, "Mã OTP khôi phục mật khẩu", buildOtpHtmlBody(otp));
        System.out.println("[EmailService] Đã gửi OTP đến: " + toEmail);
    }

    /**
     * Gửi cảnh báo vắng học bất đồng bộ — KHÔNG block UI Thread.
     *
     * Business Rules áp dụng:
     *   - Chỉ gửi khi tỷ lệ UNEXCUSED_ABSENT > 20% (strict greater-than)
     *   - Kiểm tra idempotency: không gửi lại nếu đã gửi rồi (alert_email_sent = true)
     *   - Ghi log vào collection email_alert_logs sau khi gửi
     *
     * @param studentId   ID sinh viên
     * @param studentName Tên sinh viên
     * @param toEmail     Email sinh viên
     * @param subjectCode Mã môn học
     * @param absentCount Số buổi UNEXCUSED_ABSENT
     * @param totalPlannedSessions Tổng buổi theo kế hoạch (mẫu số)
     * @param onSuccess   Callback chạy trên JavaFX thread sau khi gửi thành công
     * @param onFailure   Callback chạy trên JavaFX thread nếu gửi thất bại
     */
    public void sendAbsenceAlertAsync(
            String studentId, String studentName, String toEmail,
            String subjectCode, int absentCount, int totalPlannedSessions,
            Consumer<String> onSuccess, Consumer<String> onFailure) {

        // --- Guard: idempotency check ---
        if (isAlertAlreadySent(studentId, subjectCode)) {
            System.out.println("[EmailService] Bỏ qua — cảnh báo đã gửi cho " + studentId + " / " + subjectCode);
            return;
        }

        double absenceRate = totalPlannedSessions > 0
                ? (double) absentCount / totalPlannedSessions
                : 0.0;

        String subject = "[CẢNH BÁO CHUYÊN CẦN] Môn " + subjectCode
                + " — " + studentName + " vắng quá 20%";
        String body = buildAbsenceAlertHtmlBody(studentName, subjectCode, absentCount,
                totalPlannedSessions, absenceRate);

        // FIX TC-ABS-003: chạy trên thread pool riêng, không block JavaFX UI thread
        CompletableFuture.runAsync(() -> {
            try {
                sendEmailSync(toEmail, subject, body);
                logAlertSent(studentId, subjectCode, absentCount, totalPlannedSessions, absenceRate);
                System.out.printf("[EmailService] ✅ Cảnh báo gửi thành công: %s / %s (%.1f%%)%n",
                        studentId, subjectCode, absenceRate * 100);
                if (onSuccess != null) {
                    Platform.runLater(() -> onSuccess.accept("Email cảnh báo đã gửi đến " + toEmail));
                }
            } catch (Exception e) {
                System.err.println("[EmailService] ❌ Gửi thất bại: " + e.getMessage());
                if (onFailure != null) {
                    Platform.runLater(() -> onFailure.accept("Gửi email thất bại: " + e.getMessage()));
                }
            }
        }, EMAIL_THREAD_POOL);
    }

    // =========================================================================
    // INTERNAL HELPERS
    // =========================================================================

    /**
     * Gửi email đồng bộ — chỉ gọi từ background thread, KHÔNG phải JavaFX thread.
     */
    private void sendEmailSync(String toEmail, String subject, String htmlBody) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASS);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL, "Hệ thống TKB & Điểm danh"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setContent(htmlBody, "text/html; charset=UTF-8");

        Transport.send(message);
    }

    /**
     * FIX TC-ABS-004: Kiểm tra xem đã gửi cảnh báo cho sinh viên này trong môn này chưa.
     * Tra cứu collection email_alert_logs để tránh gửi mail trùng lặp.
     */
    private boolean isAlertAlreadySent(String studentId, String subjectCode) {
        try {
            MongoCollection<Document> logs = DatabaseHelper.getInstance()
                    .getDatabase().getCollection("email_alert_logs");
            Document existing = logs.find(
                    com.mongodb.client.model.Filters.and(
                            com.mongodb.client.model.Filters.eq("student_id", studentId),
                            com.mongodb.client.model.Filters.eq("subject_code", subjectCode),
                            com.mongodb.client.model.Filters.eq("status", "SENT_SUCCESS"),
                            com.mongodb.client.model.Filters.eq("is_revoked", false)
                    )
            ).first();
            return existing != null;
        } catch (Exception e) {
            System.err.println("[EmailService] Không kiểm tra được idempotency: " + e.getMessage());
            return false; // Fail-open: vẫn gửi nếu không check được
        }
    }

    /**
     * Ghi log vào email_alert_logs sau khi gửi thành công.
     */
    private void logAlertSent(String studentId, String subjectCode,
                              int absentCount, int totalSessions, double rate) {
        try {
            MongoCollection<Document> logs = DatabaseHelper.getInstance()
                    .getDatabase().getCollection("email_alert_logs");
            Document logDoc = new Document()
                    .append("student_id", studentId)
                    .append("subject_code", subjectCode)
                    .append("absent_count_at_send", absentCount)
                    .append("total_sessions_at_send", totalSessions)
                    .append("absence_rate_at_send", rate)
                    .append("sent_at", System.currentTimeMillis())
                    .append("status", "SENT_SUCCESS")
                    .append("triggered_by", "AUTO_SYSTEM")
                    .append("is_revoked", false);
            logs.insertOne(logDoc);
        } catch (Exception e) {
            System.err.println("[EmailService] Không ghi được email_alert_logs: " + e.getMessage());
        }
    }

    // =========================================================================
    // HTML TEMPLATES
    // =========================================================================

    private String buildOtpHtmlBody(String otp) {
        return "<div style='font-family:Arial,sans-serif;max-width:480px;margin:auto'>"
                + "<h2 style='color:#1a73e8'>Khôi phục mật khẩu</h2>"
                + "<p>Mã OTP của bạn là:</p>"
                + "<div style='font-size:36px;font-weight:bold;letter-spacing:8px;"
                + "color:#1a73e8;padding:16px;background:#f0f4ff;"
                + "border-radius:8px;text-align:center'>" + otp + "</div>"
                + "<p style='color:#888;font-size:13px'>Mã có hiệu lực trong <b>5 phút</b>. "
                + "Không chia sẻ mã này với ai.</p>"
                + "</div>";
    }

    private String buildAbsenceAlertHtmlBody(String studentName, String subjectCode,
                                             int absentCount, int totalSessions, double rate) {
        String rateStr = String.format("%.1f%%", rate * 100);
        String color = rate > 0.30 ? "#d93025" : "#f29900";
        return "<div style='font-family:Arial,sans-serif;max-width:560px;margin:auto;border:1px solid #e0e0e0;border-radius:8px;overflow:hidden'>"
                + "<div style='background:" + color + ";padding:16px 24px'>"
                + "<h2 style='color:#fff;margin:0'>⚠️ Cảnh Báo Chuyên Cần</h2>"
                + "</div>"
                + "<div style='padding:24px'>"
                + "<p>Kính gửi <b>" + studentName + "</b>,</p>"
                + "<p>Hệ thống ghi nhận bạn đã <b>vắng quá 20%</b> số buổi học môn <b>"
                + subjectCode + "</b>:</p>"
                + "<table style='border-collapse:collapse;width:100%'>"
                + "<tr style='background:#f8f9fa'><td style='padding:8px 12px;border:1px solid #dee2e6'>Môn học</td>"
                + "<td style='padding:8px 12px;border:1px solid #dee2e6'><b>" + subjectCode + "</b></td></tr>"
                + "<tr><td style='padding:8px 12px;border:1px solid #dee2e6'>Số buổi vắng (không phép)</td>"
                + "<td style='padding:8px 12px;border:1px solid #dee2e6'><b style='color:" + color + "'>"
                + absentCount + " / " + totalSessions + "</b></td></tr>"
                + "<tr style='background:#f8f9fa'><td style='padding:8px 12px;border:1px solid #dee2e6'>Tỷ lệ vắng</td>"
                + "<td style='padding:8px 12px;border:1px solid #dee2e6'><b style='color:" + color + "'>"
                + rateStr + "</b></td></tr>"
                + "</table>"
                + "<div style='margin-top:16px;padding:12px;background:#fff3e0;border-left:4px solid " + color + ";border-radius:4px'>"
                + "<b>Nguy cơ cấm thi!</b> Nếu tỷ lệ vắng tiếp tục tăng, bạn có thể bị cấm thi môn này."
                + "</div>"
                + "<p style='color:#888;font-size:12px;margin-top:16px'>Email này được gửi tự động bởi hệ thống.</p>"
                + "</div></div>";
    }
}


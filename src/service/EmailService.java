package service;

import java.util.Properties;
import jakarta.mail.*;
import jakarta.mail.internet.*;

public class EmailService {

    // ===== CẤU HÌNH GMAIL =====
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    private static final String SENDER_EMAIL = "vathe809@gmail.com"; // <-- đổi thành Gmail của bạn
    private static final String SENDER_PASS = "iqjs slke ijvl mwta"; // <-- App Password 16 ký tự

    private static final EmailService INSTANCE = new EmailService();

    public static EmailService getInstance() {
        return INSTANCE;
    }

    private EmailService() {
    }

    public void sendOTP(String toEmail, String otp) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        props.put("mail.smtp.connectiontimeout", "15000"); // 15 seconds
        props.put("mail.smtp.timeout", "15000"); // 15 seconds
        props.put("mail.smtp.writetimeout", "15000"); // 15 seconds

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASS);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL, "Hệ thống TKB & Điểm danh"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Mã OTP khôi phục mật khẩu");
        message.setContent(buildHtmlBody(otp), "text/html; charset=UTF-8");

        Transport.send(message);
        System.out.println("[EmailService] Đã gửi OTP đến: " + toEmail);
    }

    private String buildHtmlBody(String otp) {
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
}

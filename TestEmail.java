import java.util.Properties;
import jakarta.mail.*;
import jakarta.mail.internet.*;

public class TestEmail {
    public static void main(String[] args) {
        String toEmail = "nicezoe25@gmail.com";
        String otp = "123456";
        String SENDER_EMAIL = "vathe809@gmail.com";
        String SENDER_PASS = "iqjs slke ijvl mwta";
        
        System.out.println("Testing email...");
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
            props.put("mail.smtp.connectiontimeout", "15000");
            props.put("mail.smtp.timeout", "15000");

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
            message.setContent("Test OTP", "text/html; charset=UTF-8");

            Transport.send(message);
            System.out.println("Email sent successfully!");
        } catch (Exception e) {
            System.out.println("Email failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

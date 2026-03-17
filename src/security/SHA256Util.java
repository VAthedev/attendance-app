package security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class SHA256Util {

    private SHA256Util() {}

    // Hash mat khau don gian
    public static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error: " + e.getMessage());
        }
    }

    // Hash mat khau voi salt (an toan hon)
    public static String hashWithSalt(String password, String salt) {
        return hash(salt + password);
    }

    // Tao salt ngau nhien
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // Xac minh mat khau
    public static boolean verify(String password, String salt, String storedHash) {
        String hash = hashWithSalt(password, salt);
        return hash.equals(storedHash);
    }

    // Tao OTP 6 chu so
    public static String generateOTP() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}

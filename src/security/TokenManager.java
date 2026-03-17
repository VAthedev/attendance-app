package security;

import java.util.Base64;
import java.util.UUID;

public class TokenManager {

    private TokenManager() {}

    // Tao session token: base64(userId:username:uuid)
    public static String generateToken(int userId, String username) {
        String raw = userId + ":" + username + ":" + UUID.randomUUID();
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }

    // Giai ma token lay userId
    public static int getUserIdFromToken(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token));
            return Integer.parseInt(decoded.split(":")[0]);
        } catch (Exception e) {
            return -1;
        }
    }

    // Giai ma token lay username
    public static String getUsernameFromToken(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token));
            return decoded.split(":")[1];
        } catch (Exception e) {
            return null;
        }
    }
}

package database;

import model.User;
import security.SHA256Util;

import java.sql.*;

public class UserRepository {

    private final Connection conn;

    public UserRepository() throws SQLException {
        this.conn = DatabaseHelper.getInstance().getConnection();
    }

    // Dang ky user moi - nhan password thô, tu hash ben trong
    public boolean register(String username, String password, String role,
                            String fullName, String studentId, String email) throws SQLException {
        String salt = SHA256Util.generateSalt();
        String hash = SHA256Util.hashWithSalt(password, salt);

        String sql = "INSERT INTO users (username, password_hash, salt, role, full_name, student_id, email) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, salt);
            ps.setString(4, role);
            ps.setString(5, fullName);
            ps.setString(6, studentId);
            ps.setString(7, email);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                throw new SQLException("Ten dang nhap hoac email da ton tai.");
            }
            throw e;
        }
    }

    // Dang nhap - nhan password thô, lay salt tu DB roi verify
    public User login(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String salt       = rs.getString("salt");
                String storedHash = rs.getString("password_hash");

                // Lay salt tu DB, hash password + salt, so sanh
                if (SHA256Util.verify(password, salt, storedHash)) {
                    return mapUser(rs);
                } else {
                    return null; // Sai mat khau
                }
            }
        }
        return null; // Khong tim thay username
    }

    public void saveSessionToken(int userId, String token) throws SQLException {
        String sql = "UPDATE users SET session_token = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void saveDeviceId(int userId, String deviceId) throws SQLException {
        String sql = "UPDATE users SET device_id = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, deviceId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public boolean updatePassword(String email, String newPassword) throws SQLException {
        String salt = SHA256Util.generateSalt();
        String hash = SHA256Util.hashWithSalt(newPassword, salt);
        String sql  = "UPDATE users SET password_hash = ?, salt = ? WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, salt);
            ps.setString(3, email);
            return ps.executeUpdate() > 0;
        }
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        }
        return null;
    }

    public User findByToken(String token) throws SQLException {
        String sql = "SELECT * FROM users WHERE session_token = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        }
        return null;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setRole(rs.getString("role"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setStudentId(rs.getString("student_id"));
        u.setDeviceId(rs.getString("device_id"));
        u.setSessionToken(rs.getString("session_token"));
        return u;
    }
}

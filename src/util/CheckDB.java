package util;

import java.sql.*;

public class CheckDB {
    public static void main(String[] args) throws Exception {
        Connection c = DriverManager.getConnection("jdbc:sqlite:attendance.db");
        System.out.println("=== BANG USERS ===");
        ResultSet rs = c.createStatement().executeQuery(
            "SELECT id, username, role, email, password_hash, salt FROM users");
        int count = 0;
        while (rs.next()) {
            count++;
            System.out.println("ID       : " + rs.getInt("id"));
            System.out.println("Username : " + rs.getString("username"));
            System.out.println("Role     : " + rs.getString("role"));
            System.out.println("Email    : " + rs.getString("email"));
            System.out.println("Hash     : " + rs.getString("password_hash").substring(0, 16) + "...");
            System.out.println("Salt     : " + rs.getString("salt").substring(0, 8) + "...");
            System.out.println("---");
        }
        if (count == 0) System.out.println("KHONG CO USER NAO TRONG DB!");
        else System.out.println("Tong: " + count + " user");
        c.close();
    }
}

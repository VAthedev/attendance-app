package database;

import java.sql.*;
import java.io.File;

public class DatabaseHelper {

    private static final String DB_NAME = "attendance.db";
    private static DatabaseHelper instance;
    private Connection connection;

    private DatabaseHelper() {}

    // Singleton
    public static DatabaseHelper getInstance() {
        if (instance == null) {
            instance = new DatabaseHelper();
        }
        return instance;
    }

    // Lay ket noi, tu dong tao DB neu chua co
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
            initSchema();
        }
        return connection;
    }

    // Tao bang tu schema.sql neu chua ton tai
    private void initSchema() {
        String[] tables = {
            // users
            "CREATE TABLE IF NOT EXISTS users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "username TEXT UNIQUE NOT NULL," +
            "password_hash TEXT NOT NULL," +
            "salt TEXT NOT NULL," +
            "role TEXT NOT NULL CHECK(role IN ('STUDENT','LECTURER'))," +
            "full_name TEXT," +
            "email TEXT UNIQUE," +
            "student_id TEXT UNIQUE," +
            "device_id TEXT," +
            "session_token TEXT," +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)",

            // subjects
            "CREATE TABLE IF NOT EXISTS subjects (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "name TEXT NOT NULL," +
            "code TEXT UNIQUE NOT NULL," +
            "credits INTEGER DEFAULT 3," +
            "lecturer_id INTEGER REFERENCES users(id) ON DELETE SET NULL)",

            // enrollments
            "CREATE TABLE IF NOT EXISTS enrollments (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "student_id INTEGER REFERENCES users(id) ON DELETE CASCADE," +
            "subject_id INTEGER REFERENCES subjects(id) ON DELETE CASCADE," +
            "UNIQUE(student_id, subject_id))",

            // schedules
            "CREATE TABLE IF NOT EXISTS schedules (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "subject_id INTEGER REFERENCES subjects(id) ON DELETE CASCADE," +
            "day_of_week INTEGER NOT NULL CHECK(day_of_week BETWEEN 2 AND 8)," +
            "start_time TEXT NOT NULL," +
            "end_time TEXT NOT NULL," +
            "room TEXT," +
            "semester TEXT," +
            "wifi_bssid TEXT," +
            "gps_lat REAL," +
            "gps_lng REAL," +
            "gps_radius INTEGER DEFAULT 100)",

            // sessions
            "CREATE TABLE IF NOT EXISTS sessions (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "schedule_id INTEGER REFERENCES schedules(id) ON DELETE CASCADE," +
            "open_time DATETIME," +
            "close_time DATETIME," +
            "duration_minutes INTEGER DEFAULT 15," +
            "status TEXT DEFAULT 'OPEN' CHECK(status IN ('OPEN','CLOSED'))," +
            "created_by INTEGER REFERENCES users(id))",

            // attendance
            "CREATE TABLE IF NOT EXISTS attendance (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "session_id INTEGER REFERENCES sessions(id) ON DELETE CASCADE," +
            "student_id INTEGER REFERENCES users(id) ON DELETE CASCADE," +
            "check_in_time DATETIME," +
            "method TEXT CHECK(method IN ('GPS','WIFI'))," +
            "status TEXT NOT NULL CHECK(status IN ('PRESENT','LATE','ABSENT'))," +
            "device_id TEXT," +
            "gps_lat REAL," +
            "gps_lng REAL," +
            "nonce TEXT," +
            "UNIQUE(session_id, student_id)," +
            "UNIQUE(session_id, device_id))",

            // chat_messages
            "CREATE TABLE IF NOT EXISTS chat_messages (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "subject_id INTEGER REFERENCES subjects(id) ON DELETE CASCADE," +
            "sender_id INTEGER REFERENCES users(id) ON DELETE CASCADE," +
            "content TEXT NOT NULL," +
            "sent_at DATETIME DEFAULT CURRENT_TIMESTAMP)",

            // notifications
            "CREATE TABLE IF NOT EXISTS notifications (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "user_id INTEGER REFERENCES users(id) ON DELETE CASCADE," +
            "title TEXT NOT NULL," +
            "message TEXT NOT NULL," +
            "type TEXT CHECK(type IN ('ABSENCE','SCHEDULE_CHANGE','SYSTEM'))," +
            "is_read INTEGER DEFAULT 0," +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)",

            // Indexes
            "CREATE INDEX IF NOT EXISTS idx_attendance_session ON attendance(session_id)",
            "CREATE INDEX IF NOT EXISTS idx_attendance_student ON attendance(student_id)",
            "CREATE INDEX IF NOT EXISTS idx_sessions_schedule  ON sessions(schedule_id)",
            "CREATE INDEX IF NOT EXISTS idx_schedules_subject  ON schedules(subject_id)",
            "CREATE INDEX IF NOT EXISTS idx_chat_subject       ON chat_messages(subject_id)",
            "CREATE INDEX IF NOT EXISTS idx_notif_user         ON notifications(user_id)"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : tables) {
                stmt.execute(sql);
            }
            System.out.println("[DB] Schema initialized: " + DB_NAME);
        } catch (SQLException e) {
            System.err.println("[DB] Schema error: " + e.getMessage());
        }
    }

    // Dong ket noi
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Close error: " + e.getMessage());
        }
    }

    // Kiem tra ket noi
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}

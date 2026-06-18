package model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * DTO cho màn hình Lịch sử điểm danh
 */
public class AttendanceRecord {
    private final LocalDate date;
    private final String subject;
    private final String time;
    private final String method;
    private final String status;
    private final String location;

    public AttendanceRecord(LocalDate date, String subject, String time,
            String method, String status, String location) {
        this.date = date;
        this.subject = subject;
        this.time = time;
        this.method = method;
        this.status = status;
        this.location = location;
    }

    // Chuyển đổi từ Attendance entity
    public static AttendanceRecord fromAttendance(Attendance att, String subjectName) {
        String timeStr = att.getCheckInTime() != null
                ? att.getCheckInTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                : "--:--:--";

        return new AttendanceRecord(
                att.getAttendanceDate(),
                subjectName,
                timeStr,
                att.getMethod(),
                att.getStatus(),
                att.getLocation());
    }

    // Getters
    public LocalDate getDate() {
        return date;
    }

    public String getSubject() {
        return subject;
    }

    public String getTime() {
        return time;
    }

    public String getMethod() {
        return method;
    }

    public String getStatus() {
        return status;
    }

    public String getLocation() {
        return location;
    }
}
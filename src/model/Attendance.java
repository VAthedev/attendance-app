package model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Attendance {

    private int id;
    private int userId;
    private int scheduleId;
    private LocalDate attendanceDate;
    private LocalTime checkInTime;
    private String status; // PRESENT, ABSENT, LATE
    private String method; // GPS, WiFi, QR
    private String location; // GPS coordinates or WiFi SSID
    private String notes;
    private LocalTime createdAt;

    public Attendance() {
    }

    public Attendance(int userId, int scheduleId, LocalDate attendanceDate,
            LocalTime checkInTime, String status, String method, String location) {
        this.userId = userId;
        this.scheduleId = scheduleId;
        this.attendanceDate = attendanceDate;
        this.checkInTime = checkInTime;
        this.status = status;
        this.method = method;
        this.location = location;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public String getStatus() {
        return status;
    }

    public String getMethod() {
        return method;
    }

    public String getLocation() {
        return location;
    }

    public String getNotes() {
        return notes;
    }

    public LocalTime getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public void setCheckInTime(LocalTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreatedAt(LocalTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Attendance{" +
                "id=" + id +
                ", userId=" + userId +
                ", scheduleId=" + scheduleId +
                ", attendanceDate=" + attendanceDate +
                ", status='" + status + '\'' +
                ", method='" + method + '\'' +
                '}';
    }
}

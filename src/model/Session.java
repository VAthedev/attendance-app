package model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Session {

    private int id;
    private int scheduleId;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status; // ACTIVE, CLOSED, CANCELLED
    private String attendanceMethod; // GPS, WiFi, QR, MANUAL
    private int totalStudents;
    private int presentCount;
    private int absentCount;
    private int lateCount;

    public Session() {
    }

    public Session(int scheduleId, LocalDate sessionDate,
            LocalTime startTime, LocalTime endTime) {
        this.scheduleId = scheduleId;
        this.sessionDate = sessionDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = "ACTIVE";
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public String getAttendanceMethod() {
        return attendanceMethod;
    }

    public int getTotalStudents() {
        return totalStudents;
    }

    public int getPresentCount() {
        return presentCount;
    }

    public int getAbsentCount() {
        return absentCount;
    }

    public int getLateCount() {
        return lateCount;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAttendanceMethod(String attendanceMethod) {
        this.attendanceMethod = attendanceMethod;
    }

    public void setTotalStudents(int totalStudents) {
        this.totalStudents = totalStudents;
    }

    public void setPresentCount(int presentCount) {
        this.presentCount = presentCount;
    }

    public void setAbsentCount(int absentCount) {
        this.absentCount = absentCount;
    }

    public void setLateCount(int lateCount) {
        this.lateCount = lateCount;
    }

    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", scheduleId=" + scheduleId +
                ", sessionDate=" + sessionDate +
                ", status='" + status + '\'' +
                '}';
    }
}

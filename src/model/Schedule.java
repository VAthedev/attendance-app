package model;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class Schedule {

    private int id;
    private int subjectId;
    private int lecturerId;
    private String room;
    private DayOfWeek dayOfWeek; // THỨ (Monday, Tuesday, ...)
    private LocalTime startTime;
    private LocalTime endTime;
    private int semester;
    private int academicYear;
    private String status; // ACTIVE, INACTIVE
    private Double latitude; // GPS cho kiểm tra vị trí
    private Double longitude;
    private String wifiSSID; // WiFi SSID cho kiểm tra

    public Schedule() {
    }

    public Schedule(int subjectId, int lecturerId, String room,
            DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.subjectId = subjectId;
        this.lecturerId = lecturerId;
        this.room = room;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public int getLecturerId() {
        return lecturerId;
    }

    public String getRoom() {
        return room;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public int getSemester() {
        return semester;
    }

    public int getAcademicYear() {
        return academicYear;
    }

    public String getStatus() {
        return status;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getWifiSSID() {
        return wifiSSID;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public void setLecturerId(int lecturerId) {
        this.lecturerId = lecturerId;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public void setAcademicYear(int academicYear) {
        this.academicYear = academicYear;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setWifiSSID(String wifiSSID) {
        this.wifiSSID = wifiSSID;
    }

    @Override
    public String toString() {
        return "Schedule{" +
                "id=" + id +
                ", subjectId=" + subjectId +
                ", room='" + room + '\'' +
                ", dayOfWeek=" + dayOfWeek +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}

package service;

import model.Attendance;
import model.Schedule;
import model.Subject;
import model.User;
import database.AttendanceRepository;
import org.bson.Document;
import protocol.Request;
import protocol.Response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AttendanceService {

    private AttendanceRepository attendanceRepository;

    public AttendanceService() {
        this.attendanceRepository = new AttendanceRepository();
    }

    /**
     * Lấy lịch sử điểm danh của sinh viên
     */
    public List<Attendance> getAttendanceHistory(String studentId) {
        List<Document> docs = attendanceRepository.findByStudentId(studentId);
        return convertDocumentsToAttendance(docs);
    }

    /**
     * Convert MongoDB Document to Attendance model
     */
    private List<Attendance> convertDocumentsToAttendance(List<Document> docs) {
        List<Attendance> attendances = new ArrayList<>();
        for (Document doc : docs) {
            Attendance att = new Attendance();
            att.setId(doc.getString("_id").hashCode());
            att.setUserId(doc.getString("student_id").hashCode());
            att.setScheduleId(doc.getString("session_id").hashCode());
            att.setStatus(doc.getString("status"));
            att.setMethod(doc.getString("method"));
            att.setLocation(doc.getString("location"));
            att.setNotes(doc.getString("notes"));
            attendances.add(att);
        }
        return attendances;
    }

    /**
     * Lấy lịch sử điểm danh của sinh viên theo khoảng thời gian
     */
    public List<Attendance> getAttendanceHistoryByDateRange(String studentId, LocalDate startDate, LocalDate endDate) {
        List<Attendance> allRecords = getAttendanceHistory(studentId);
        List<Attendance> filtered = new ArrayList<>();

        for (Attendance record : allRecords) {
            if (record.getAttendanceDate() != null &&
                    !record.getAttendanceDate().isBefore(startDate) &&
                    !record.getAttendanceDate().isAfter(endDate)) {
                filtered.add(record);
            }
        }

        return filtered;
    }

    /**
     * Lấy lịch sử điểm danh theo tháng
     */
    public List<Attendance> getAttendanceByMonth(String studentId, int month, int year) {
        List<Attendance> allRecords = getAttendanceHistory(studentId);
        List<Attendance> filtered = new ArrayList<>();

        for (Attendance record : allRecords) {
            if (record.getAttendanceDate() != null &&
                    record.getAttendanceDate().getMonthValue() == month &&
                    record.getAttendanceDate().getYear() == year) {
                filtered.add(record);
            }
        }

        return filtered;
    }

    /**
     * Lấy lịch sử điểm danh theo môn học
     */
    public List<Attendance> getAttendanceBySubject(String studentId, String subjectId) {
        List<Attendance> allRecords = getAttendanceHistory(studentId);
        List<Attendance> filtered = new ArrayList<>();

        for (Attendance record : allRecords) {
            // Cần join với Schedule để lấy subjectId
            // Placeholder: sẽ implement sau khi có database connection
        }

        return filtered;
    }

    /**
     * Lấy thống kê chuyên cần theo học kỳ
     */
    public AttendanceStatistics getStatisticsBySemester(String studentId, int semester) {
        List<Attendance> records = getAttendanceHistory(studentId);

        AttendanceStatistics stats = new AttendanceStatistics();

        int totalPresent = 0;
        int totalAbsent = 0;
        int totalLate = 0;

        for (Attendance record : records) {
            if ("PRESENT".equals(record.getStatus())) {
                totalPresent++;
            } else if ("ABSENT".equals(record.getStatus())) {
                totalAbsent++;
            } else if ("LATE".equals(record.getStatus())) {
                totalLate++;
            }
        }

        int total = totalPresent + totalAbsent + totalLate;
        stats.setTotalSessions(total);
        stats.setPresentCount(totalPresent);
        stats.setAbsentCount(totalAbsent);
        stats.setLateCount(totalLate);
        stats.setAttendancePercentage(total > 0 ? (double) totalPresent / total * 100 : 0);

        return stats;
    }

    /**
     * Kiểm tra xem sinh viên có đã điểm danh buổi này chưa
     */
    public boolean isAlreadyAttended(String studentId, String sessionId) {
        Document doc = attendanceRepository.findBySessionAndStudent(sessionId, studentId);
        return doc != null;
    }

    /**
     * Ghi nhận điểm danh cho sinh viên
     */
    public boolean recordAttendance(Document attendanceDoc) {
        try {
            // Validate dữ liệu
            String studentId = attendanceDoc.getString("student_id");
            String sessionId = attendanceDoc.getString("session_id");

            if (studentId == null || studentId.isBlank() ||
                    sessionId == null || sessionId.isBlank()) {
                return false;
            }

            // Kiểm tra xem đã điểm danh rồi
            if (isAlreadyAttended(studentId, sessionId)) {
                return false;
            }

            // Lưu vào database
            return attendanceRepository.insert(attendanceDoc);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy tỷ lệ chuyên cần trung bình
     */
    public double getAverageAttendanceRate(String studentId) {
        List<Attendance> records = getAttendanceHistory(studentId);

        if (records.isEmpty()) {
            return 0;
        }

        long presentCount = records.stream()
                .filter(r -> "PRESENT".equals(r.getStatus()))
                .count();

        return (double) presentCount / records.size() * 100;
    }

    /**
     * Inner class cho thống kê chuyên cần
     */
    public static class AttendanceStatistics {
        private int totalSessions;
        private int presentCount;
        private int absentCount;
        private int lateCount;
        private double attendancePercentage;

        public int getTotalSessions() {
            return totalSessions;
        }

        public void setTotalSessions(int totalSessions) {
            this.totalSessions = totalSessions;
        }

        public int getPresentCount() {
            return presentCount;
        }

        public void setPresentCount(int presentCount) {
            this.presentCount = presentCount;
        }

        public int getAbsentCount() {
            return absentCount;
        }

        public void setAbsentCount(int absentCount) {
            this.absentCount = absentCount;
        }

        public int getLateCount() {
            return lateCount;
        }

        public void setLateCount(int lateCount) {
            this.lateCount = lateCount;
        }

        public double getAttendancePercentage() {
            return attendancePercentage;
        }

        public void setAttendancePercentage(double attendancePercentage) {
            this.attendancePercentage = attendancePercentage;
        }
    }
}

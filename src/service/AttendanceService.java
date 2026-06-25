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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import database.SessionRepository;

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
        SessionRepository sessionRepo = SessionRepository.getInstance();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Document doc : docs) {
            Attendance att = new Attendance();
            
            Object idObj = doc.get("_id");
            att.setId(idObj != null ? idObj.toString().hashCode() : 0);
            
            Object studentIdObj = doc.get("student_id");
            att.setUserId(studentIdObj != null ? studentIdObj.toString().hashCode() : 0);
            
            String sessionId = doc.getString("session_id");
            if (sessionId != null) {
                att.setScheduleId(sessionId.hashCode());
                
                // Fetch session
                Document sessionDoc = sessionRepo.findById(sessionId);
                if (sessionDoc != null) {
                    att.setSubjectName(sessionDoc.getString("subject"));
                    att.setRoom(sessionDoc.getString("room"));
                    
                    Long startTimeMs = sessionDoc.getLong("start_time");
                    Long endTimeMs = sessionDoc.getLong("end_time");
                    if (startTimeMs != null) {
                        Instant instant = Instant.ofEpochMilli(startTimeMs);
                        LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
                        LocalTime time = instant.atZone(ZoneId.systemDefault()).toLocalTime();
                        att.setAttendanceDate(date);
                        att.setTimestamp(startTimeMs);
                        
                        String tString = time.format(timeFormatter);
                        if (endTimeMs != null) {
                            LocalTime eTime = Instant.ofEpochMilli(endTimeMs).atZone(ZoneId.systemDefault()).toLocalTime();
                            tString += " - " + eTime.format(timeFormatter);
                        }
                        att.setTimeString(tString);
                    }
                }
            } else {
                // Read from new schema
                Long ts = doc.getLong("timestamp");
                if (ts != null) {
                    att.setTimestamp(ts);
                    Instant instant = Instant.ofEpochMilli(ts);
                    LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
                    att.setAttendanceDate(date);
                }
                
                String subjectCode = doc.getString("subject_code");
                if (subjectCode != null) {
                    // Try to resolve subject name
                    Document subjDoc = database.DatabaseHelper.getInstance().getSubjectsCollection().find(new Document("code", subjectCode)).first();
                    if (subjDoc != null) {
                        att.setSubjectName(subjDoc.getString("name"));
                    } else {
                        att.setSubjectName(subjectCode);
                    }
                }
            }
            
            att.setStatus(doc.getString("status"));
            att.setMethod(doc.getString("method"));
            
            String location = doc.getString("location");
            att.setLocation(location != null ? location : "");
            att.setNotes(doc.getString("notes"));
            
            attendances.add(att);
        }
        
        // Sắp xếp giảm dần theo ngày
        attendances.sort((a, b) -> {
            if (a.getAttendanceDate() == null && b.getAttendanceDate() == null) return 0;
            if (a.getAttendanceDate() == null) return 1;
            if (b.getAttendanceDate() == null) return -1;
            return b.getAttendanceDate().compareTo(a.getAttendanceDate());
        });
        
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
     * Finalize attendance for a session, mark absent students, and send warnings if needed.
     */
    public void finalizeSessionAttendance(String sessionId) {
        Document sessionDoc = SessionRepository.getInstance().findById(sessionId);
        if (sessionDoc == null) return;
        
        String classCode = sessionDoc.getString("class_name");
        String subjectCode = sessionDoc.getString("subject");
        if (classCode == null) return;
        
        List<Document> enrollments = database.EnrollmentRepository.getInstance().findStudentsByClassCode(classCode);
        List<Document> attendances = attendanceRepository.findBySessionId(sessionId);
        
        NotificationService notifService = new NotificationService();
        long now = System.currentTimeMillis();
        
        for (Document enr : enrollments) {
            String studentId = enr.getString("student_id");
            if (studentId == null) continue;
            
            boolean attended = attendances.stream().anyMatch(a -> studentId.equals(a.getString("student_id")));
            
            if (!attended) {
                // Insert ABSENT record
                Document absentDoc = new Document()
                        .append("session_id", sessionId)
                        .append("student_id", studentId)
                        .append("subject_code", subjectCode)
                        .append("class_code", classCode)
                        .append("method", "SYSTEM")
                        .append("status", "ABSENT")
                        .append("timestamp", now);
                attendanceRepository.insert(absentDoc);
                
                // Count total absences for this student in this subject
                List<Document> historyDocs = attendanceRepository.findByStudentId(studentId);
                long absentCount = historyDocs.stream()
                        .filter(a -> "ABSENT".equals(a.getString("status")))
                        .filter(a -> subjectCode != null && subjectCode.equals(a.getString("subject_code")))
                        .count() + 1; // +1 for the one we just inserted because historyDocs might not reflect it immediately if we just inserted it
                
                if (absentCount >= 2) {
                    notifService.sendAbsenceWarning(studentId, subjectCode, (int) absentCount);
                }
            }
        }
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

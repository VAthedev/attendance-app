package service;

import model.Attendance;
import database.AttendanceRepository;
import database.DatabaseHelper;
import org.bson.Document;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import database.SessionRepository;

// ===========================================================================
// BUSINESS RULES (áp dụng từ 2026-07-02):
//   1. Chỉ UNEXCUSED_ABSENT bị tính vào tỷ lệ % vắng (EXCUSED_ABSENT không tính)
//   2. Mẫu số = total_sessions_planned từ collection subjects (theo kế hoạch)
//   3. Ngưỡng STRICT: absenceRate > 0.20 (đúng 20% = an toàn; 20.1% = cảnh báo)
// ===========================================================================

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
            } else if ("ABSENT".equals(record.getStatus()) || "UNEXCUSED_ABSENT".equals(record.getStatus())) {
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

        long validRecords = records.stream()
                .filter(r -> !"EXCUSED_ABSENT".equals(r.getStatus()))
                .count();
                
        if (validRecords == 0) return 0;

        long presentCount = records.stream()
                .filter(r -> "PRESENT".equals(r.getStatus()))
                .count();

        return (double) presentCount / validRecords * 100;
    }

    /**
     * Finalize attendance for a session:
     *   - Đánh dấu UNEXCUSED_ABSENT cho sinh viên không điểm danh
     *   - Tính tỷ lệ vắng dựa trên UNEXCUSED_ABSENT / total_sessions_planned
     *   - Kích hoạt cảnh báo async nếu tỷ lệ > 20% (STRICT)
     *
     * Business Rules:
     *   BR-1: Chỉ UNEXCUSED_ABSENT tính vào % vắng
     *   BR-2: Mẫu số = subjects.total_sessions_planned (không phải số buổi thực tế)
     *   BR-3: Threshold: absenceRate STRICTLY > 0.20 (đúng 20.0% = an toàn)
     */
    public void finalizeSessionAttendance(String sessionId) {
        Document sessionDoc = SessionRepository.getInstance().findById(sessionId);
        if (sessionDoc == null) return;

        String classCode   = sessionDoc.getString("class_name");
        String subjectCode = sessionDoc.getString("subject");
        if (classCode == null) return;

        List<Document> enrollments = database.EnrollmentRepository.getInstance().findStudentsByClassCode(classCode);
        List<Document> attendances = attendanceRepository.findBySessionId(sessionId);

        // BR-2: Lấy tổng buổi theo kế hoạch từ subjects collection
        int totalPlannedSessions = getPlannedSessionCount(subjectCode, classCode);

        long now = System.currentTimeMillis();

        for (Document enr : enrollments) {
            String studentId = enr.getString("student_id");
            if (studentId == null) continue;

            boolean attended = attendances.stream()
                    .anyMatch(a -> studentId.equals(a.getString("student_id")));

            if (!attended) {
                // BR-1: Ghi trạng thái UNEXCUSED_ABSENT (không phép) — duy nhất loại này tính vào %
                Document absentDoc = new Document()
                        .append("session_id",  sessionId)
                        .append("student_id",  studentId)
                        .append("subject_code", subjectCode)
                        .append("class_code",   classCode)
                        .append("method",       "SYSTEM")
                        .append("status",       "UNEXCUSED_ABSENT")  // BR-1
                        .append("timestamp",    now);
                attendanceRepository.insert(absentDoc);

                // Tính tỷ lệ vắng chỉ với UNEXCUSED_ABSENT (BR-1)
                List<Document> historyDocs = attendanceRepository.findByStudentId(studentId);
                long unexcusedAbsentCount = historyDocs.stream()
                        .filter(a -> "UNEXCUSED_ABSENT".equals(a.getString("status")))
                        .filter(a -> subjectCode != null && subjectCode.equals(a.getString("subject_code")))
                        .count() + 1; // +1 vì bản ghi vừa insert chưa phản ánh ngay

                // BR-3: Strict > 20% (không phải >=)
                if (totalPlannedSessions > 0) {
                    double absenceRate = (double) unexcusedAbsentCount / totalPlannedSessions;
                    if (absenceRate > 0.20) {  // STRICT greater-than
                        triggerAbsenceAlertAsync(studentId, subjectCode,
                                (int) unexcusedAbsentCount, totalPlannedSessions);
                    }
                }
            }
        }
    }

    /**
     * Lấy tổng số buổi học theo kế hoạch từ subjects.total_sessions_planned.
     * Fallback: đếm số session CLOSED trong classCode nếu không có trường này.
     * BR-2: dùng theo kế hoạch để tránh False Positive đầu học kỳ.
     */
    private int getPlannedSessionCount(String subjectCode, String classCode) {
        if (subjectCode == null) return 0;
        try {
            Document subjectDoc = DatabaseHelper.getInstance()
                    .getSubjectsCollection()
                    .find(com.mongodb.client.model.Filters.eq("code", subjectCode))
                    .first();
            if (subjectDoc != null) {
                Integer planned = subjectDoc.getInteger("total_sessions_planned");
                if (planned != null && planned > 0) return planned;
            }
        } catch (Exception e) {
            System.err.println("[AttendanceService] Không lấy được total_sessions_planned: " + e.getMessage());
        }
        // Fallback: đếm CLOSED sessions thực tế
        try {
            return (int) DatabaseHelper.getInstance().getSessionsCollection()
                    .countDocuments(com.mongodb.client.model.Filters.and(
                            com.mongodb.client.model.Filters.eq("class_name", classCode),
                            com.mongodb.client.model.Filters.eq("subject", subjectCode),
                            com.mongodb.client.model.Filters.eq("status", "CLOSED")
                    ));
        } catch (Exception e) {
            System.err.println("[AttendanceService] Fallback count cũng thất bại: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Kích hoạt gửi email cảnh báo bất đồng bộ (KHÔNG block UI thread).
     * Tra cứu email sinh viên từ users collection, sau đó gọi EmailService.sendAbsenceAlertAsync().
     */
    private void triggerAbsenceAlertAsync(String studentId, String subjectCode,
                                          int absentCount, int totalPlannedSessions) {
        try {
            Document userDoc = DatabaseHelper.getInstance().getUsersCollection()
                    .find(com.mongodb.client.model.Filters.or(
                            com.mongodb.client.model.Filters.eq("id", studentId),
                            com.mongodb.client.model.Filters.eq("_id", studentId)
                    )).first();
            if (userDoc == null) {
                System.err.println("[AttendanceService] Không tìm thấy user: " + studentId);
                return;
            }
            String email     = userDoc.getString("email");
            String fullName  = userDoc.getString("full_name");
            if (email == null || email.isBlank()) return;

            EmailService.getInstance().sendAbsenceAlertAsync(
                    studentId, fullName != null ? fullName : studentId, email,
                    subjectCode, absentCount, totalPlannedSessions,
                    msg -> System.out.println("[AttendanceService] Email OK: " + msg),
                    err -> System.err.println("[AttendanceService] Email FAIL: " + err)
            );
        } catch (Exception e) {
            System.err.println("[AttendanceService] triggerAbsenceAlertAsync lỗi: " + e.getMessage());
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

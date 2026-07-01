package ai;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import database.DatabaseHelper;
import dev.langchain4j.agent.tool.Tool;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class DatabaseTools {

    private final DatabaseHelper db;

    public DatabaseTools() {
        this.db = DatabaseHelper.getInstance();
    }

    @Tool("Lấy danh sách các môn học mà sinh viên đang đăng ký dựa vào mã sinh viên (studentId)")
    public String getRegisteredSubjects(String studentId) {
        MongoCollection<Document> enrollments = db.getEnrollmentsCollection();
        MongoCollection<Document> subjects = db.getSubjectsCollection();

        List<String> subjectIds = new ArrayList<>();
        try (MongoCursor<Document> cursor = enrollments.find(new Document("student_id", studentId)).iterator()) {
            while (cursor.hasNext()) {
                subjectIds.add(cursor.next().getString("subject_id"));
            }
        }

        if (subjectIds.isEmpty()) {
            return "Sinh viên không đăng ký môn học nào.";
        }

        JSONArray result = new JSONArray();
        for (String subjId : subjectIds) {
            Document query = new Document();
            if (ObjectId.isValid(subjId)) {
                query.put("_id", new ObjectId(subjId));
            } else {
                query.put("_id", subjId);
            }
            
            Document subjDoc = subjects.find(query).first();
            if (subjDoc != null) {
                JSONObject item = new JSONObject();
                item.put("mã môn", subjDoc.getString("code"));
                item.put("tên môn", subjDoc.getString("name"));
                item.put("tín chỉ", subjDoc.getInteger("credits", 0));
                result.put(item);
            }
        }
        return result.toString();
    }

    @Tool("Lấy lịch học chi tiết của các môn học mà sinh viên đã đăng ký (truyền vào studentId)")
    public String getStudentSchedule(String studentId) {
        MongoCollection<Document> enrollments = db.getEnrollmentsCollection();
        MongoCollection<Document> schedules = db.getSchedulesCollection();
        MongoCollection<Document> subjects = db.getSubjectsCollection();

        List<String> subjectIds = new ArrayList<>();
        try (MongoCursor<Document> cursor = enrollments.find(new Document("student_id", studentId)).iterator()) {
            while (cursor.hasNext()) {
                subjectIds.add(cursor.next().getString("subject_id"));
            }
        }

        if (subjectIds.isEmpty()) {
            return "Không có lịch học nào.";
        }

        JSONArray result = new JSONArray();
        for (String subjId : subjectIds) {
            String subjName = subjId;
            Document query = new Document();
            if (ObjectId.isValid(subjId)) {
                query.put("_id", new ObjectId(subjId));
            } else {
                query.put("_id", subjId);
            }
            
            Document subjDoc = subjects.find(query).first();
            if (subjDoc != null) {
                subjName = subjDoc.getString("name");
            }

            try (MongoCursor<Document> schedCursor = schedules.find(new Document("subject_id", subjId)).iterator()) {
                while (schedCursor.hasNext()) {
                    Document schedDoc = schedCursor.next();
                    JSONObject item = new JSONObject();
                    item.put("môn học", subjName);
                    item.put("phòng", schedDoc.getString("room"));
                    item.put("thứ", schedDoc.getInteger("day_of_week", 0));
                    item.put("ca học", schedDoc.getString("shift"));
                    result.put(item);
                }
            }
        }
        return result.toString();
    }

    @Tool("Lấy thông tin điểm danh của sinh viên (số buổi vắng, số buổi đi học) dựa vào mã sinh viên (studentId)")
    public String getStudentAttendanceSummary(String studentId) {
        MongoCollection<Document> enrollments = db.getEnrollmentsCollection();
        MongoCollection<Document> sessions = db.getSessionsCollection();
        MongoCollection<Document> attendance = db.getAttendanceCollection();
        MongoCollection<Document> subjects = db.getSubjectsCollection();

        List<String> subjectIds = new ArrayList<>();
        try (MongoCursor<Document> cursor = enrollments.find(new Document("student_id", studentId)).iterator()) {
            while (cursor.hasNext()) {
                subjectIds.add(cursor.next().getString("subject_id"));
            }
        }

        if (subjectIds.isEmpty()) {
            return "Sinh viên chưa đăng ký môn học nào nên không có dữ liệu điểm danh.";
        }

        JSONArray result = new JSONArray();
        for (String subjId : subjectIds) {
            String subjName = subjId;
            String className = "";
            Document query = new Document();
            if (ObjectId.isValid(subjId)) {
                query.put("_id", new ObjectId(subjId));
            } else {
                query.put("_id", subjId);
            }
            
            Document subjDoc = subjects.find(query).first();
            if (subjDoc != null) {
                subjName = subjDoc.getString("name");
            }

            // Tìm tất cả các phiên điểm danh của môn này (dựa vào danh sách schedule của môn, hoặc query theo subject_id)
            // Trong hệ thống này, session lưu class_name hoặc schedule_id. 
            // Tạm thời lấy tất cả session có className khớp với lớp sinh viên đăng ký (nếu có lưu)
            // Để đơn giản, ta sẽ đếm tổng số session đã mở, và số session sinh viên có mặt
            
            // Lấy class_code từ enrollment để query session
            Document enrollQuery = new Document("student_id", studentId).append("subject_id", subjId);
            Document enrollDoc = enrollments.find(enrollQuery).first();
            if (enrollDoc != null && enrollDoc.getString("class_code") != null) {
                className = enrollDoc.getString("class_code");
            }

            long totalSessions = 0;
            if (!className.isEmpty()) {
                totalSessions = sessions.countDocuments(new Document("class_name", className));
            }

            // Đếm số buổi có mặt
            long attendedSessions = 0;
            if (totalSessions > 0) {
                // Lấy danh sách session_id của lớp này
                List<String> sessionIds = new ArrayList<>();
                try (MongoCursor<Document> sessCursor = sessions.find(new Document("class_name", className)).iterator()) {
                    while (sessCursor.hasNext()) {
                        Document sDoc = sessCursor.next();
                        if (sDoc.getObjectId("_id") != null) {
                            sessionIds.add(sDoc.getObjectId("_id").toHexString());
                        } else {
                            sessionIds.add(sDoc.getString("_id"));
                        }
                    }
                }
                
                // Đếm trong bảng attendance xem sinh viên có bao nhiêu record thuộc các sessionIds này
                attendedSessions = attendance.countDocuments(
                    new Document("student_id", studentId).append("session_id", new Document("$in", sessionIds))
                );
            }

            long absentSessions = totalSessions - attendedSessions;

            JSONObject item = new JSONObject();
            item.put("môn học", subjName);
            item.put("lớp", className);
            item.put("tổng số buổi đã điểm danh", totalSessions);
            item.put("số buổi có mặt", attendedSessions);
            item.put("số buổi vắng", absentSessions);
            if (totalSessions > 0 && absentSessions >= totalSessions * 0.2) {
                item.put("cảnh báo", "Có nguy cơ cấm thi do vắng quá 20%");
            }
            result.put(item);
        }
        return result.toString();
    }

    @Tool("Lấy điểm thi và điểm tổng kết của sinh viên (truyền vào studentId)")
    public String getStudentGrades(String studentId) {
        // App hiện tại chưa có database lưu điểm số, trả về dữ liệu giả lập (mock data)
        return "[Hệ thống điểm số đang bảo trì] Hiện tại dữ liệu điểm thi học kỳ chưa được đồng bộ từ phòng đào tạo. Vui lòng kiểm tra lại sau hoặc liên hệ phòng giáo vụ để biết điểm chính xác.";
    }

    @Tool("Lấy lịch thi cuối kỳ của sinh viên (truyền vào studentId)")
    public String getExamSchedules(String studentId) {
        // Trả về dữ liệu giả lập cho lịch thi
        return "Lịch thi dự kiến: Tuần sau sinh viên chưa có lịch thi nào. Lịch thi chính thức sẽ được công bố trước ngày thi 2 tuần. Vui lòng theo dõi email của trường để nhận thông báo.";
    }

    @Tool("Lấy thông tin Kế hoạch đào tạo năm học 2026-2027 của trường (thời gian học kỳ, ngày nghỉ lễ, nghỉ tết, lịch đăng ký môn, lễ tốt nghiệp, v.v...)")
    public String getAcademicCalendar() {
        return "THÔNG TIN KẾ HOẠCH ĐÀO TẠO NĂM HỌC 2026-2027 (TRƯỜNG ĐH CÔNG NGHỆ THÔNG TIN):\n" +
               "- Học kỳ 1 (HK1): Bắt đầu từ tháng 9/2026 đến cuối tháng 1/2027. Thi cuối kỳ HK1 diễn ra vào cuối tháng 12/2026 đến giữa tháng 1/2027.\n" +
               "- Học kỳ 2 (HK2): Bắt đầu từ cuối tháng 2/2027 đến tháng 6/2027. Thi cuối kỳ HK2 vào khoảng tháng 6/2027.\n" +
               "- Học kỳ Hè: Bắt đầu từ tháng 7/2027 đến tháng 8/2027. Thi cuối kỳ Hè vào cuối tháng 8/2027.\n\n" +
               "CÁC NGÀY NGHỈ LỄ & NGHỈ TẾT:\n" +
               "- Lễ Quốc khánh: Thứ Tư, ngày 02/09/2026.\n" +
               "- Tết Dương lịch: Thứ Sáu, ngày 01/01/2027.\n" +
               "- Tết Âm lịch: Nghỉ khoảng 3-4 tuần trong tháng 2/2027 (Mùng 1 Tết là Thứ Bảy, 06/02/2027). Lưu ý: Tuần trước và ngay sau Nghỉ Tết Âm lịch, giảng viên sẽ giảng dạy bằng hình thức trực tuyến (Online).\n" +
               "- Giỗ tổ Hùng Vương: Thứ Tư, ngày 17/03/2027 (10/03 Âm lịch).\n" +
               "- Giải phóng miền Nam: Thứ Sáu, ngày 30/04/2027.\n" +
               "- Quốc tế Lao động: Thứ Bảy, ngày 01/05/2027.\n\n" +
               "CÁC MỐC THỜI GIAN QUAN TRỌNG KHÁC:\n" +
               "- Lễ Khai giảng: Tổ chức vào buổi sáng ngày 05/09/2026.\n" +
               "- Tân sinh viên Khóa 2026: Xác nhận nhập học (13-21/8/2026); Kiểm tra Tiếng Anh đầu khóa (26-27/8 Nghe Đọc Viết, 03/9 Nói).\n" +
               "- Đăng ký học phần (ĐKHP): Tháng 8/2026 (cho HK1), Tháng 1/2027 (cho HK2) và giữa tháng 6/2027 (cho HK Hè).\n" +
               "- Khảo sát các môn sẽ mở: Tháng 5/2027.\n" +
               "- Lễ Tốt nghiệp (Lễ TN): Tổ chức 2 đợt chính vào giữa tháng 11/2026 và giữa tháng 6/2027.";
    }

    @Tool("Lấy thông tin liên lạc (Email và Microsoft Teams) của giảng viên dựa vào tên (ví dụ: 'Nguyễn Ngọc Tự', 'Trần Hữu Nghị'). Nếu người dùng hỏi cách liên hệ hoặc xin email giảng viên thì dùng công cụ này.")
    public String getLecturerContactInfo(String lecturerName) {
        if (lecturerName == null || lecturerName.trim().isEmpty()) {
            return "Không có tên giảng viên để tạo email.";
        }
        
        String normalized = java.text.Normalizer.normalize(lecturerName.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("đ", "d");
                
        String[] parts = normalized.split("\\s+");
        if (parts.length < 2) {
            return "Tên giảng viên không hợp lệ (cần có ít nhất Họ và Tên).";
        }
        
        String givenName = parts[parts.length - 1];
        StringBuilder prefix = new StringBuilder(givenName);
        prefix.append(parts[0].charAt(0)); // Họ
        for (int i = 1; i < parts.length - 1; i++) { // Tên đệm
            prefix.append(parts[i].charAt(0));
        }
        
        String account = prefix.toString();
        String email = account + "@uit.edu.vn";
        String teams = account + "@hcmuit.edu.vn";
        
        return "Thông tin liên lạc của giảng viên " + lecturerName + ":\n" +
               "- Thư điện tử (Email): " + email + "\n" +
               "- Tài khoản Microsoft Teams: " + teams;
    }
}

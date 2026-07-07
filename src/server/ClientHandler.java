package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import database.UserRepository;
import model.ChatMessage;
import model.User;
import protocol.Request;
import protocol.Response;
import security.SHA256Util;
import security.TokenManager;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    // userId nếu authenticated
    private String userId;
    private String authenticatedStudentId;
    private String userName;
    private final Set<String> joinedRooms = new HashSet<>();

    private static final Map<String, String> otpStore = new java.util.concurrent.ConcurrentHashMap<>();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            // Register writer for notifications (userId set after login)

            String line;
            while ((line = reader.readLine()) != null) {
                Request req = Request.fromJson(line);
                Response res = handleRequest(req);
                // Correlate response to request by adding nonce
                if (res != null) {
                    res.setNonce(req.getNonce());
                }
                writer.println(res.toJson());
            }

        } catch (IOException e) {
            System.err.println("[Handler] Client ngat ket noi: " + e.getMessage());
        } finally {
            close();
        }
    }

    private Response handleRequest(Request req) {
        try {
            switch (req.getRequestType()) {
                case LOGIN:
                    return handleLogin(req);
                case REGISTER:
                    return handleRegister(req);
                case FORGOT_PASSWORD:
                    return handleForgotPassword(req);
                case VERIFY_OTP:
                    return handleVerifyOTP(req);
                case RESET_PASSWORD:
                    return handleResetPassword(req);
                case SEND_CHAT:
                    return handleSendChat(req);
                case GET_CHAT_HISTORY:
                    return handleGetChatHistory(req);
                case GET_CHAT_ROOMS:
                    return handleGetChatRooms(req);
                case PING:
                    return Response.ok("pong");
                case GET_SCHEDULE_BY_DAY:
                    return handleGetScheduleByDay(req);
                case GET_SCHEDULE_BY_WEEK:
                case GET_SCHEDULE_BY_SUBJECT:
                    return handleGetScheduleByRange(req);
                case SYNC_SCHEDULE:
                    return handleSyncSchedule(req);
                case OPEN_SESSION:
                    return handleOpenSession(req);
                case CLOSE_SESSION:
                    return handleCloseSession(req);
                case SUBMIT_ATTENDANCE:
                    return handleSubmitAttendance(req);
                case GET_ATTENDANCE_HISTORY:
                    return handleGetAttendanceHistory(req);
                case GET_ATTENDANCE_STATS:
                    return handleGetAttendanceStats(req);
                case GET_NOTIFICATIONS:
                    return handleGetNotifications(req);
                case MARK_NOTIFICATION_READ:
                    return handleMarkNotificationRead(req);
                case GET_LECTURER_CLASSES:
                    return handleGetLecturerClasses(req);
                case GET_LECTURER_SCHEDULE:
                    return handleGetLecturerSchedule(req);
                case GET_LECTURER_TODAY_SCHEDULE:
                    return handleGetLecturerTodaySchedule(req);
                case GET_LECTURER_DASHBOARD_STATS:
                    return handleGetLecturerDashboardStats(req);
                case GET_ACTIVE_SESSIONS:
                    return handleGetActiveSessions(req);
                case GET_LECTURER_ATTENDANCE_LIST:
                    return handleGetLecturerAttendanceList(req);
                case GET_LECTURER_EXPORT_DATA:
                    return handleGetLecturerExportData(req);
                case GET_LECTURER_STATS:
                    return handleGetLecturerStats(req);
                case GET_ACTIVE_SESSION_FOR_STUDENT:
                    return handleGetActiveSessionForStudent(req);
                case GET_STUDENT_TODAY_SCHEDULE:
                    return handleGetStudentTodaySchedule(req);
                case GET_STUDENT_STATS:
                    return handleGetStudentStats(req);
                case AI_TOOL_QUERY:
                    return handleAiToolQuery(req);
                default:
                    return Response.error("Chua ho tro: " + req.getType());
            }
        } catch (Exception e) {
            System.err.println("[Handler] Loi xu ly: " + e.getMessage());
            return Response.error("Loi server: " + e.getMessage());
        }
    }

    private Response handleLogin(Request req) throws SQLException {
        String username = req.getPayloadValue("username");
        String password = req.getPayloadValue("password");
        if (username == null || username.isEmpty())
            return Response.error("Thieu username.");
        if (password == null || password.isEmpty())
            return Response.error("Thieu password.");

        UserRepository repo = new UserRepository();
        User user = repo.login(username, password);

        if (user == null) {
            return Response.error("Ten dang nhap hoac mat khau khong dung.");
        }

        String token = TokenManager.generateToken(user.getId(), user.getUsername());
        repo.saveSessionToken(user.getId(), token);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("role", user.getRole());
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("fullName", user.getFullName() != null ? user.getFullName() : "");
        data.put("studentId", user.getStudentId() != null ? user.getStudentId() : "");
        data.put("requirePasswordChange", user.isRequirePasswordChange());

        // Lưu userId để đăng ký writer cho notifications/broadcast
        this.userId = String.valueOf(user.getId());
        this.authenticatedStudentId = firstNonBlank(user.getStudentId(), this.userId);
        this.userName = user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName()
                : user.getUsername();
        BroadcastManager.getInstance().registerUserWriter(this.userId, this.writer);
        return Response.ok(data);
    }

    private Response handleRegister(Request req) throws SQLException {
        String username = req.getPayloadValue("username");
        String password = req.getPayloadValue("password");
        String role = req.getPayloadValue("role");
        String fullName = req.getPayloadValue("fullName");
        String studentId = req.getPayloadValue("studentId");
        String email = req.getPayloadValue("email");

        if (username == null || username.isEmpty())
            return Response.error("Thieu username.");
        if (password == null || password.isEmpty())
            return Response.error("Thieu password.");
        if (role == null || role.isEmpty())
            return Response.error("Thieu role.");

        if (!role.equals("STUDENT") && !role.equals("LECTURER"))
            return Response.error("Role khong hop le: " + role);

        UserRepository repo = new UserRepository();
        repo.register(username, password, role, fullName, studentId, email);
        return Response.ok("Dang ky thanh cong.");
    }

    private Response handleForgotPassword(Request req) throws Exception {
        String email = req.getPayloadValue("email");
        if (email == null || email.isBlank())
            return Response.error("Thiếu email.");

        System.out.println("[OTP Trace] 1. Bắt đầu tìm user trong MongoDB...");
        UserRepository repo = new UserRepository();
        User user = repo.findByEmail(email);
        if (user == null) {
            System.out.println("[OTP Trace] Không tìm thấy user với email: " + email);
            return Response.error("Email không tồn tại trong hệ thống.");
        }

        System.out.println("[OTP Trace] 2. Đã tìm thấy user, chuẩn bị gửi OTP qua Email...");
        String otp = SHA256Util.generateOTP();
        otpStore.put(email, otp);

        try {
            service.EmailService.getInstance().sendOTP(email, otp);
            System.out.println("[OTP Trace] 3. Đã gửi OTP thành công qua Email!");
        } catch (Exception e) {
            System.err.println("[Handler] Lỗi gửi email: " + e.getMessage());
            return Response.error("Không thể gửi email. Vui lòng thử lại.");
        }

        return Response.ok("OTP đã gửi đến " + email);
    }

    private Response handleVerifyOTP(Request req) {
        String email = req.getPayloadValue("email");
        String otp = req.getPayloadValue("otp");
        if (email == null || otp == null)
            return Response.error("Thieu thong tin.");

        String stored = otpStore.get(email);
        if (stored == null)
            return Response.error("OTP het han.");
        if (!stored.equals(otp))
            return Response.error("OTP khong dung.");

        otpStore.remove(email);
        return Response.ok("Xac nhan thanh cong.");
    }

    private Response handleResetPassword(Request req) throws SQLException {
        String email = req.getPayloadValue("email");
        String username = req.getPayloadValue("username");
        String password = req.getPayloadValue("password");
        if ((email == null && username == null) || password == null)
            return Response.error("Thieu thong tin.");

        UserRepository repo = new UserRepository();
        boolean ok = false;
        if (email != null && !email.isBlank()) {
            ok = repo.updatePassword(email, password);
        } else if (username != null && !username.isBlank()) {
            ok = repo.updatePasswordByUsername(username, password);
        }
        
        return ok ? Response.ok("Cap nhat mat khau thanh cong.")
                : Response.error("Khong tim thay tai khoan.");
    }

    private Response handleSendChat(Request req) {
        String roomKey = req.getPayloadValue("roomKey");
        String roomName = req.getPayloadValue("roomName");
        String sender = req.getPayloadValue("sender");
        String senderName = req.getPayloadValue("senderName");
        String content = req.getPayloadValue("content");
        if (roomKey == null || sender == null || content == null)
            return Response.error("Thieu thong tin chat.");

        // Gọi ChatService để xử lý và broadcast
        try {
            joinRoom(roomKey);
            ChatMessage msg = service.ChatService.getInstance().sendMessage(
                    roomKey,
                    roomName != null ? roomName : roomKey,
                    sender,
                    senderName != null ? senderName : this.userName,
                    content);
            Response res = Response.ok("Sent");
            res.putPayload("message_id", msg.getId());
            res.putPayload("roomKey", msg.getRoomKey());
            res.putPayload("roomName", msg.getRoomName());
            res.putPayload("timestamp", msg.getTimestamp());
            return res;
        } catch (Exception e) {
            return Response.error("Loi khi gui chat: " + e.getMessage());
        }
    }

    private Response handleGetChatHistory(Request req) {
        String roomKey = req.getPayloadValue("roomKey");
        String roomName = req.getPayloadValue("roomName");
        if (roomKey == null || roomKey.isBlank()) {
            return Response.error("Thieu roomKey.");
        }

        try {
            joinRoom(roomKey);
            List<ChatMessage> history = service.ChatService.getInstance().loadRecentHistory(roomKey, 30);
            StringBuilder sb = new StringBuilder();
            for (ChatMessage msg : history) {
                if (sb.length() > 0)
                    sb.append("\n");
                sb.append(msg.getTimestamp())
                        .append("|")
                        .append(escapePipe(msg.getSenderName() != null ? msg.getSenderName() : msg.getSender()))
                        .append("|")
                        .append(escapePipe(msg.getContent()));
            }

            Response res = Response.ok("CHAT_HISTORY");
            res.putPayload("roomKey", roomKey);
            res.putPayload("roomName", roomName != null ? roomName : roomKey);
            res.putPayload("days", 30);
            res.putPayload("count", history.size());
            res.putPayload("history", sb.toString());
            return res;
        } catch (Exception e) {
            return Response.error("Loi tai history: " + e.getMessage());
        }
    }

    private Response handleGetChatRooms(Request req) {
        try {
            java.util.List<java.util.Map<String, Object>> rooms = service.ChatService.getInstance().loadRooms();
            org.json.JSONArray arr = new org.json.JSONArray();
            for (java.util.Map<String, Object> room : rooms) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("roomKey", safe(room.get("roomKey")));
                obj.put("roomName", safe(room.get("roomName")));
                obj.put("subjectCode", safe(room.get("subjectCode")));
                obj.put("subjectName", safe(room.get("subjectName")));
                arr.put(obj);
            }
            Response res = Response.ok("CHAT_ROOMS");
            res.putPayload("rooms", arr.toString());
            return res;
        } catch (Exception e) {
            return Response.error("Loi tai phong chat: " + e.getMessage());
        }
    }

    private void joinRoom(String roomKey) {
        if (roomKey == null || roomKey.isBlank())
            return;
        if (joinedRooms.add(roomKey)) {
            BroadcastManager.getInstance().registerClientInRoom(roomKey, this);
        }
    }

    private String escapePipe(String text) {
        if (text == null)
            return "";
        return text.replace("|", "\\|").replace("\n", " ").replace("\r", " ");
    }

    private Response handleGetAttendanceHistory(Request req) {
        String studentId = firstNonBlank(req.getPayloadValue("studentId"), userId);
        if (studentId == null) {
            return Response.error("Thieu studentId.");
        }

        try {
            java.util.List<model.Attendance> records = new service.AttendanceService().getAttendanceHistory(studentId);
            Response res = Response.ok("ATTENDANCE_HISTORY");
            res.putPayload("records", attendanceArray(records).toString());
            return res;
        } catch (Exception e) {
            return Response.error("Loi lay lich su diem danh: " + e.getMessage());
        }
    }

    private Response handleGetAttendanceStats(Request req) {
        String studentId = firstNonBlank(req.getPayloadValue("studentId"), userId);
        if (studentId == null) {
            return Response.error("Thieu studentId.");
        }

        try {
            java.util.List<model.Attendance> records = new service.AttendanceService().getAttendanceHistory(studentId);
            Response res = Response.ok("ATTENDANCE_STATS");
            res.putPayload("records", attendanceArray(records).toString());
            res.putPayload("total", records.size());
            res.putPayload("present", records.stream().filter(r -> "PRESENT".equals(r.getStatus())).count());
            res.putPayload("absent", records.stream().filter(r -> "ABSENT".equals(r.getStatus()) || "UNEXCUSED_ABSENT".equals(r.getStatus())).count());
            res.putPayload("late", records.stream().filter(r -> "LATE".equals(r.getStatus())).count());
            return res;
        } catch (Exception e) {
            return Response.error("Loi lay thong ke diem danh: " + e.getMessage());
        }
    }

    private Response handleGetNotifications(Request req) {
        String studentId = firstNonBlank(req.getPayloadValue("studentId"), userId);
        if (studentId == null) {
            return Response.error("Thieu studentId.");
        }

        try {
            java.util.List<model.Notification> notifications = new service.NotificationService().getNotificationsForStudent(studentId);
            org.json.JSONArray arr = new org.json.JSONArray();
            for (model.Notification notification : notifications) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("id", safe(notification.getId()));
                obj.put("studentId", safe(notification.getStudentId()));
                obj.put("title", safe(notification.getTitle()));
                obj.put("message", safe(notification.getMessage()));
                obj.put("type", safe(notification.getType()));
                obj.put("isRead", notification.isRead());
                obj.put("createdAt", notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : "");
                arr.put(obj);
            }
            Response res = Response.ok("NOTIFICATIONS");
            res.putPayload("notifications", arr.toString());
            return res;
        } catch (Exception e) {
            return Response.error("Loi lay thong bao: " + e.getMessage());
        }
    }

    private Response handleMarkNotificationRead(Request req) {
        try {
            service.NotificationService notificationService = new service.NotificationService();
            String all = req.getPayloadValue("all");
            if ("true".equalsIgnoreCase(all)) {
                String studentId = firstNonBlank(req.getPayloadValue("studentId"), userId);
                if (studentId == null) return Response.error("Thieu studentId.");
                notificationService.markAllAsRead(studentId);
            } else {
                String notificationId = req.getPayloadValue("notificationId");
                if (notificationId == null || notificationId.isBlank()) return Response.error("Thieu notificationId.");
                notificationService.markAsRead(notificationId);
            }
            return Response.ok("Da cap nhat thong bao.");
        } catch (Exception e) {
            return Response.error("Loi cap nhat thong bao: " + e.getMessage());
        }
    }

    private Response handleGetLecturerClasses(Request req) {
        String lecturerName = req.getPayloadValue("lecturerName");
        if (lecturerName == null || lecturerName.isBlank()) {
            return Response.error("Thieu lecturerName.");
        }

        try {
            java.util.List<String> classes = database.ScheduleRepository.getInstance().findUniqueClassesByLecturerName(lecturerName);
            Response res = Response.ok("LECTURER_CLASSES");
            res.putPayload("classes", new org.json.JSONArray(classes).toString());
            return res;
        } catch (Exception e) {
            return Response.error("Loi lay danh sach lop: " + e.getMessage());
        }
    }

    private Response handleGetLecturerSchedule(Request req) {
        String lecturerName = req.getPayloadValue("lecturerName");
        String startStr = req.getPayloadValue("startDate");
        String endStr = req.getPayloadValue("endDate");
        if (lecturerName == null || startStr == null || endStr == null) {
            return Response.error("Thieu lecturerName/startDate/endDate.");
        }

        try {
            java.time.LocalDate start = java.time.LocalDate.parse(startStr);
            java.time.LocalDate end = java.time.LocalDate.parse(endStr);
            java.util.List<java.util.Map<String, Object>> schedules =
                    database.ScheduleRepository.getInstance().findLecturerSchedulesInRange(lecturerName, start, end);
            Response res = Response.ok("LECTURER_SCHEDULE");
            res.putPayload("schedules", scheduleArray(schedules).toString());
            return res;
        } catch (Exception e) {
            return Response.error("Loi lay lich giang vien: " + e.getMessage());
        }
    }

    private Response handleGetLecturerTodaySchedule(Request req) {
        String lecturerName = req.getPayloadValue("lecturerName");
        if (lecturerName == null || lecturerName.isBlank()) {
            return Response.error("Thieu lecturerName.");
        }

        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.util.List<java.util.Map<String, Object>> schedules =
                    database.ScheduleRepository.getInstance().findLecturerSchedulesByDate(lecturerName, today);
            org.json.JSONArray arr = new org.json.JSONArray();
            for (java.util.Map<String, Object> schedule : schedules) {
                String className = safe(schedule.get("className"));
                org.json.JSONObject obj = scheduleToJson(schedule);
                obj.put("time", formatPeriods(safe(schedule.get("periods"))));
                obj.put("students", database.EnrollmentRepository.getInstance().countStudentsByClassCode(className) + " SV");
                arr.put(obj);
            }
            Response res = Response.ok("LECTURER_TODAY_SCHEDULE");
            res.putPayload("items", arr.toString());
            return res;
        } catch (Exception e) {
            return Response.error("Loi lay lich hom nay: " + e.getMessage());
        }
    }

    private Response handleGetLecturerDashboardStats(Request req) {
        String lecturerName = req.getPayloadValue("lecturerName");
        if (lecturerName == null || lecturerName.isBlank()) {
            return Response.error("Thieu lecturerName.");
        }

        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.util.List<java.util.Map<String, Object>> schedules =
                    database.ScheduleRepository.getInstance().findLecturerSchedulesByDate(lecturerName, today);
            long totalStudents = 0;
            for (java.util.Map<String, Object> schedule : schedules) {
                totalStudents += database.EnrollmentRepository.getInstance().countStudentsByClassCode(safe(schedule.get("className")));
            }
            Response res = Response.ok("LECTURER_DASHBOARD_STATS");
            res.putPayload("totalSubjects", schedules.size());
            res.putPayload("totalStudents", totalStudents);
            res.putPayload("avgAttendance", "--%");
            res.putPayload("absentWarning", 0);
            return res;
        } catch (Exception e) {
            return Response.error("Loi lay dashboard stats: " + e.getMessage());
        }
    }

    private Response handleGetActiveSessions(Request req) {
        String lecturerId = firstNonBlank(req.getPayloadValue("lecturerId"), userId);
        if (lecturerId == null) {
            return Response.error("Thieu lecturerId.");
        }

        try {
            java.util.List<org.bson.Document> activeSessions = database.SessionRepository.getInstance().findActiveSessions(lecturerId);
            org.json.JSONArray arr = new org.json.JSONArray();
            for (org.bson.Document session : activeSessions) {
                String sessionId = documentId(session);
                String className = session.getString("class_name");
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("sessionId", sessionId);
                obj.put("subject", safe(session.getString("subject")));
                obj.put("status", "Dang mo");
                obj.put("openTime", formatTime(documentLong(session, "start_time")));
                obj.put("checkedIn", new database.AttendanceRepository().countBySessionId(sessionId));
                obj.put("total", database.EnrollmentRepository.getInstance().countStudentsByClassCode(className));
                arr.put(obj);
            }
            Response res = Response.ok("ACTIVE_SESSIONS");
            res.putPayload("sessions", arr.toString());
            return res;
        } catch (Exception e) {
            return Response.error("Loi lay phien dang mo: " + e.getMessage());
        }
    }

    private Response handleGetLecturerAttendanceList(Request req) {
        String lecturerId = firstNonBlank(req.getPayloadValue("lecturerId"), userId);
        if (lecturerId == null) {
            return Response.error("Thieu lecturerId.");
        }

        try {
            java.util.List<org.bson.Document> sessions = database.SessionRepository.getInstance().findAllSessionsByLecturerId(lecturerId);
            database.AttendanceRepository attendanceRepository = new database.AttendanceRepository();
            org.json.JSONArray arr = new org.json.JSONArray();
            for (org.bson.Document doc : sessions) {
                String sessionId = documentId(doc);
                String className = firstNonBlank(doc.getString("class_name"), doc.getString("class_code"));
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("id", sessionId);
                obj.put("className", safe(className));
                obj.put("subject", safe(firstNonBlank(doc.getString("subject"), doc.getString("subject_code"))));
                obj.put("date", sessionDate(doc));
                obj.put("startTime", sessionTime(doc, "start_time"));
                obj.put("endTime", sessionTime(doc, "end_time"));
                obj.put("status", safe(doc.getString("status")));
                obj.put("presentCount", attendanceRepository.countBySessionId(sessionId));
                obj.put("totalStudents", database.EnrollmentRepository.getInstance().countStudentsByClassCode(className));
                arr.put(obj);
            }
            Response res = Response.ok("LECTURER_ATTENDANCE_LIST");
            res.putPayload("sessions", arr.toString());
            return res;
        } catch (Exception e) {
            return Response.error("Loi lay danh sach diem danh: " + e.getMessage());
        }
    }

    private Response handleGetLecturerExportData(Request req) {
        String classCode = extractClassCode(req.getPayloadValue("classSelection"));
        if (classCode == null || classCode.isBlank()) {
            return Response.error("Thieu classCode.");
        }

        try {
            java.util.List<org.bson.Document> enrollments = database.EnrollmentRepository.getInstance().findStudentsByClassCode(classCode);
            java.util.List<org.bson.Document> sessions = database.SessionRepository.getInstance().findSessionsByClassCode(classCode);
            java.util.Map<String, org.json.JSONObject> studentMap = new java.util.HashMap<>();
            for (org.bson.Document enrollment : enrollments) {
                String studentId = enrollment.getString("student_id");
                org.bson.Document studentDetails = enrollment.get("student_details", org.bson.Document.class);
                String studentName = studentDetails != null ? studentDetails.getString("full_name") : "Khong ro";
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("studentId", safe(studentId));
                obj.put("studentName", safe(studentName));
                obj.put("totalSessions", sessions.size());
                obj.put("presentCount", 0);
                obj.put("absentCount", 0);
                studentMap.put(studentId, obj);
            }

            database.AttendanceRepository attendanceRepository = new database.AttendanceRepository();
            for (org.bson.Document session : sessions) {
                String sessionId = documentId(session);
                for (org.bson.Document attendance : attendanceRepository.findBySessionId(sessionId)) {
                    String studentId = attendance.getString("student_id");
                    org.json.JSONObject data = studentMap.get(studentId);
                    if (data != null) {
                        if ("PRESENT".equals(attendance.getString("status"))) {
                            data.put("presentCount", data.optInt("presentCount") + 1);
                        } else {
                            data.put("absentCount", data.optInt("absentCount") + 1);
                        }
                    }
                }
            }

            org.json.JSONArray arr = new org.json.JSONArray();
            studentMap.values().stream()
                    .sorted(java.util.Comparator.comparing(o -> o.optString("studentId")))
                    .forEach(arr::put);
            Response res = Response.ok("LECTURER_EXPORT_DATA");
            res.putPayload("students", arr.toString());
            return res;
        } catch (Exception e) {
            return Response.error("Loi lay du lieu xuat file: " + e.getMessage());
        }
    }

    private Response handleGetLecturerStats(Request req) {
        String classCode = extractClassCode(req.getPayloadValue("classSelection"));
        if (classCode == null || classCode.isBlank()) {
            return Response.error("Thieu classCode.");
        }

        try {
            long totalStudents = database.EnrollmentRepository.getInstance().countStudentsByClassCode(classCode);
            java.util.List<org.bson.Document> sessions = database.SessionRepository.getInstance().findSessionsByClassCode(classCode);
            database.AttendanceRepository attendanceRepository = new database.AttendanceRepository();

            org.json.JSONArray bars = new org.json.JSONArray();
            long totalPresent = 0;
            for (org.bson.Document session : sessions) {
                String sessionId = documentId(session);
                long presentCount = attendanceRepository.findBySessionId(sessionId).stream()
                        .filter(a -> "PRESENT".equals(a.getString("status")))
                        .count();
                totalPresent += presentCount;
                org.json.JSONObject bar = new org.json.JSONObject();
                bar.put("label", sessionDate(session));
                bar.put("presentCount", presentCount);
                bars.put(bar);
            }

            long totalPossible = totalStudents * sessions.size();
            long totalAbsent = Math.max(0, totalPossible - totalPresent);
            double avgRate = totalPossible > 0 ? (double) totalPresent / totalPossible * 100 : 0;

            Response res = Response.ok("LECTURER_STATS");
            res.putPayload("totalStudents", totalStudents);
            res.putPayload("totalSessions", sessions.size());
            res.putPayload("avgRate", avgRate);
            res.putPayload("totalPresent", totalPresent);
            res.putPayload("totalAbsent", totalAbsent);
            res.putPayload("bars", bars.toString());
            return res;
        } catch (Exception e) {
            return Response.error("Loi lay thong ke giang vien: " + e.getMessage());
        }
    }

    private Response handleGetActiveSessionForStudent(Request req) {
        String studentId = firstNonBlank(req.getPayloadValue("studentId"), userId);
        if (studentId == null) {
            return Response.error("Thieu studentId.");
        }

        try {
            org.bson.Document activeSession = findActiveSessionForStudent(studentId);
            Response res = Response.ok("ACTIVE_SESSION_FOR_STUDENT");
            if (activeSession != null) {
                res.putPayload("session", sessionToJson(activeSession).toString());
            } else {
                res.putPayload("session", "");
            }
            return res;
        } catch (Exception e) {
            return Response.error("Loi lay phien diem danh: " + e.getMessage());
        }
    }

    private Response handleGetStudentTodaySchedule(Request req) {
        String studentId = firstNonBlank(req.getPayloadValue("studentId"), userId);
        if (studentId == null) {
            return Response.error("Thieu studentId.");
        }

        try {
            java.util.List<java.util.Map<String, Object>> schedules =
                    database.ScheduleRepository.getInstance().findStudentSchedulesByDate(studentId, java.time.LocalDate.now());
            Response res = Response.ok("STUDENT_TODAY_SCHEDULE");
            res.putPayload("schedules", scheduleArray(schedules).toString());
            return res;
        } catch (Exception e) {
            return Response.error("Loi lay lich sinh vien hom nay: " + e.getMessage());
        }
    }

    private Response handleGetStudentStats(Request req) {
        String studentId = firstNonBlank(req.getPayloadValue("studentId"), userId);
        if (studentId == null) {
            return Response.error("Thieu studentId.");
        }

        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.util.List<java.util.Map<String, Object>> schedules =
                    database.ScheduleRepository.getInstance().findStudentSchedulesInRange(studentId, today.minusWeeks(2), today.plusWeeks(10));
            long subjectCount = schedules.stream().map(s -> safe(s.get("subject"))).distinct().count();

            service.AttendanceService attendanceService = new service.AttendanceService();
            double avgRate = attendanceService.getAverageAttendanceRate(studentId);
            java.util.List<model.Attendance> monthlyRecords = attendanceService.getAttendanceByMonth(studentId, today.getMonthValue(), today.getYear());
            long absentCount = monthlyRecords.stream()
                    .filter(r -> "ABSENT".equals(r.getStatus()) || "UNEXCUSED_ABSENT".equals(r.getStatus()))
                    .count();
            long lateCount = monthlyRecords.stream().filter(r -> "LATE".equals(r.getStatus())).count();

            Response res = Response.ok("STUDENT_STATS");
            res.putPayload("totalSubjects", subjectCount);
            res.putPayload("attendanceRate", avgRate);
            res.putPayload("absentCount", absentCount);
            res.putPayload("lateCount", lateCount);
            return res;
        } catch (Exception e) {
            return Response.error("Loi lay thong ke sinh vien: " + e.getMessage());
        }
    }

    private Response handleAiToolQuery(Request req) {
        String tool = req.getPayloadValue("tool");
        String input = req.getPayloadValue("input");
        if (tool == null || tool.isBlank()) {
            return Response.error("Thieu tool.");
        }

        try {
            String result = switch (tool) {
                case "registeredSubjects" -> aiRegisteredSubjects(input);
                case "studentSchedule" -> aiStudentSchedule(input);
                case "studentAttendanceSummary" -> aiStudentAttendanceSummary(input);
                case "lecturerSchedule" -> aiLecturerSchedule(input);
                default -> "Cong cu AI chua duoc ho tro: " + tool;
            };
            Response res = Response.ok("AI_TOOL_RESULT");
            res.putPayload("result", result);
            return res;
        } catch (Exception e) {
            return Response.error("Loi AI tool: " + e.getMessage());
        }
    }

    private Response handleOpenSession(Request req) {
        if (userId == null) {
            return Response.error("Chưa đăng nhập.");
        }
        
        try {
            org.bson.Document sessionDoc = new org.bson.Document();
            sessionDoc.put("lecturer_id", userId);
            sessionDoc.put("class_name", req.getPayloadValue("class_name"));
            sessionDoc.put("subject", req.getPayloadValue("subject"));
            
            int duration = Integer.parseInt(req.getPayloadValue("duration")); // minutes
            sessionDoc.put("duration", duration);
            
            long serverNow = System.currentTimeMillis();
            Long requestedStartTime = null;
            if (req.getPayload().containsKey("start_time")) {
                try {
                    requestedStartTime = Long.parseLong(req.getPayloadValue("start_time"));
                } catch (NumberFormatException ignored) {}
            }
            long startTime = serverNow;
            sessionDoc.put("start_time", startTime);
            sessionDoc.put("end_time", startTime + (duration * 60L * 1000L));
            if (requestedStartTime != null) {
                sessionDoc.put("requested_start_time", requestedStartTime);
            }
            
            sessionDoc.put("room", req.getPayloadValue("room"));
            sessionDoc.put("gps_enabled", Boolean.parseBoolean(req.getPayloadValue("gps_enabled")));
            sessionDoc.put("wifi_enabled", Boolean.parseBoolean(req.getPayloadValue("wifi_enabled")));
            sessionDoc.put("gps_lat", req.getPayloadValue("gps_lat"));
            sessionDoc.put("gps_lng", req.getPayloadValue("gps_lng"));
            sessionDoc.put("gps_radius", req.getPayloadValue("gps_radius"));
            sessionDoc.put("wifi_bssid", req.getPayloadValue("wifi_bssid"));
            
            String sessionId = database.SessionRepository.getInstance().openSession(sessionDoc);
            
            // Lên lịch tự động đóng phiên
            SessionCountdownService.getInstance().scheduleSessionClose(sessionId, duration);
            
            String className = req.getPayloadValue("class_name");
            String subject = req.getPayloadValue("subject");
            java.util.List<org.bson.Document> enrolledStudents = database.EnrollmentRepository.getInstance().findStudentsByClassCode(className);
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            
            for (org.bson.Document studentDoc : enrolledStudents) {
                String studentId = studentDoc.getString("student_id");
                if (studentId != null) {
                    database.NotificationRepository.getInstance().insertNotification(
                        studentId,
                        "Phiên điểm danh mới",
                        "Giảng viên vừa mở điểm danh cho lớp " + className + " - " + subject + ".",
                        "SYSTEM",
                        now
                    );
                    BroadcastManager.getInstance().sendNotificationToUser(
                        studentId,
                        "Phiên điểm danh mới",
                        "Giảng viên vừa mở điểm danh cho lớp " + className + " - " + subject + "."
                    );
                }
            }
            
            // Phục vụ cho UI sinh viên auto-reload tab Điểm danh nếu đúng lớp
            BroadcastManager.getInstance().broadcastAnnouncement("SESSION_OPENED:" + className);
            Response res = Response.ok("Mở phiên điểm danh thành công.");
            res.putPayload("session_id", sessionId);
            res.putPayload("start_time", startTime);
            res.putPayload("end_time", startTime + (duration * 60L * 1000L));
            res.putPayload("server_time", System.currentTimeMillis());
            return res;
        } catch (Exception e) {
            return Response.error("Lỗi khi mở phiên: " + e.getMessage());
        }
    }

    private Response handleCloseSession(Request req) {
        if (userId == null) {
            return Response.error("Chưa đăng nhập.");
        }
        String sessionId = req.getPayloadValue("session_id");
        if (sessionId == null) {
            return Response.error("Thiếu session_id.");
        }
        boolean success = database.SessionRepository.getInstance().closeSession(sessionId);
        if (success) {
            new service.AttendanceService().finalizeSessionAttendance(sessionId);
            BroadcastManager.getInstance().broadcastAnnouncement("SESSION_CLOSED:" + sessionId);
            return Response.ok("Đã đóng phiên điểm danh.");
        } else {
            return Response.error("Không thể đóng phiên (không tồn tại hoặc đã đóng).");
        }
    }

    private Response handleSyncSchedule(Request req) {
        System.out.println("[ClientHandler] Yêu cầu đồng bộ TKB từ " + userId);
        BroadcastManager.getInstance().broadcastAnnouncement("SCHEDULE_UPDATED");
        return Response.ok("Yêu cầu đồng bộ TKB đã được phát đi.");
    }

    private Response handleGetScheduleByDay(Request req) {
        String sid = req.getPayloadValue("studentId");
        String dateStr = req.getPayloadValue("date");
        if (sid == null || dateStr == null) return Response.error("Thiếu thông tin studentId hoặc date.");

        try {
            java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
            List<Map<String,Object>> rows = database.ScheduleRepository.getInstance().findStudentSchedulesByDate(sid, date);
            
            org.json.JSONArray arr = new org.json.JSONArray();
            for (Map<String,Object> r : rows) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("subject", r.getOrDefault("subject", ""));
                obj.put("startTime", r.getOrDefault("startTime", ""));
                obj.put("endTime", r.getOrDefault("endTime", ""));
                obj.put("lecturer", r.getOrDefault("lecturer", ""));
                obj.put("room", r.getOrDefault("room", ""));
                obj.put("className", r.getOrDefault("className", ""));
                obj.put("status", r.getOrDefault("status", "PENDING"));
                obj.put("attendanceTime", r.getOrDefault("attendanceTime", ""));
                
                String className = (String) r.getOrDefault("className", "");
                boolean isSessionOpen = false;
                List<org.bson.Document> activeSessions = database.SessionRepository.getInstance().findSessionsByClassCode(className);
                long currentMillis = System.currentTimeMillis();
                for (org.bson.Document s : activeSessions) {
                    if ("OPEN".equals(s.getString("status"))) {
                        Long sEndTime = s.getLong("end_time");
                        if (sEndTime != null && sEndTime > currentMillis) {
                            isSessionOpen = true;
                            break;
                        }
                    }
                }
                obj.put("isSessionOpen", isSessionOpen);
                arr.put(obj);
            }
            
            Response res = Response.ok("SCHEDULE_DAY");
            res.putPayload("schedules", arr.toString());
            return res;
        } catch (Exception e) {
            return Response.error("Lỗi lấy TKB: " + e.getMessage());
        }
    }

    private Response handleGetScheduleByRange(Request req) {
        String sid = req.getPayloadValue("studentId");
        String startStr = req.getPayloadValue("startDate");
        String endStr = req.getPayloadValue("endDate");
        if (sid == null || startStr == null || endStr == null) return Response.error("Thiếu thông tin studentId hoặc date.");

        try {
            java.time.LocalDate startDate = java.time.LocalDate.parse(startStr);
            java.time.LocalDate endDate = java.time.LocalDate.parse(endStr);
            List<Map<String,Object>> rows = database.ScheduleRepository.getInstance().findStudentSchedulesInRange(sid, startDate, endDate);
            
            service.AttendanceService attendanceService = new service.AttendanceService();
            List<model.Attendance> attendances = attendanceService.getAttendanceHistory(sid);
            
            org.json.JSONArray arr = new org.json.JSONArray();
            for (Map<String,Object> r : rows) {
                org.json.JSONObject obj = new org.json.JSONObject();
                String dateStr = (String) r.getOrDefault("date", "");
                obj.put("date", dateStr);
                
                String subj = (String) r.getOrDefault("subject", "");
                obj.put("subject", subj);
                obj.put("startTime", r.getOrDefault("startTime", ""));
                obj.put("endTime", r.getOrDefault("endTime", ""));
                obj.put("lecturer", r.getOrDefault("lecturer", ""));
                obj.put("room", r.getOrDefault("room", ""));
                obj.put("className", r.getOrDefault("className", ""));
                
                java.time.LocalDate date = null;
                try { date = java.time.LocalDate.parse(dateStr); } catch (Exception ex) {}
                
                String status = "PENDING";
                if (date != null) {
                    for (model.Attendance att : attendances) {
                        if (att.getSubjectName() != null && att.getSubjectName().equals(subj)) {
                            java.time.LocalDate attDate = java.time.Instant.ofEpochMilli(att.getTimestamp()).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                            if (attDate.equals(date)) {
                                if ("PRESENT".equals(att.getStatus())) status = "ATTENDED";
                                else if ("ABSENT".equals(att.getStatus())) status = "ABSENT";
                                else if ("LATE".equals(att.getStatus())) status = "LATE";
                            }
                        }
                    }
                    if ("PENDING".equals(status) && date.isBefore(java.time.LocalDate.now())) {
                        status = "ABSENT";
                    }
                }
                obj.put("status", status);
                
                arr.put(obj);
            }
            
            Response res = Response.ok("SCHEDULE_RANGE");
            res.putPayload("schedules", arr.toString());
            return res;
        } catch (Exception e) {
            return Response.error("Lỗi lấy TKB range: " + e.getMessage());
        }
    }

    private org.json.JSONArray scheduleArray(java.util.List<java.util.Map<String, Object>> schedules) {
        org.json.JSONArray arr = new org.json.JSONArray();
        for (java.util.Map<String, Object> schedule : schedules) {
            arr.put(scheduleToJson(schedule));
        }
        return arr;
    }

    private org.json.JSONObject scheduleToJson(java.util.Map<String, Object> schedule) {
        org.json.JSONObject obj = new org.json.JSONObject();
        obj.put("date", safe(schedule.get("date")));
        obj.put("subject", safe(schedule.get("subject")));
        obj.put("startTime", safe(schedule.get("startTime")));
        obj.put("endTime", safe(schedule.get("endTime")));
        obj.put("lecturer", safe(schedule.get("lecturer")));
        obj.put("room", safe(schedule.get("room")));
        obj.put("className", safe(schedule.get("className")));
        obj.put("periods", safe(schedule.get("periods")));
        obj.put("status", safe(schedule.getOrDefault("status", "PENDING")));
        obj.put("attendanceTime", safe(schedule.get("attendanceTime")));
        return obj;
    }

    private org.json.JSONArray attendanceArray(java.util.List<model.Attendance> records) {
        org.json.JSONArray arr = new org.json.JSONArray();
        for (model.Attendance attendance : records) {
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("date", attendance.getAttendanceDate() != null ? attendance.getAttendanceDate().toString() : "");
            obj.put("subjectName", safe(attendance.getSubjectName()));
            obj.put("timeString", safe(attendance.getTimeString()));
            obj.put("method", safe(attendance.getMethod()));
            obj.put("status", safe(attendance.getStatus()));
            obj.put("room", safe(attendance.getRoom()));
            obj.put("location", safe(attendance.getLocation()));
            obj.put("timestamp", attendance.getTimestamp());
            arr.put(obj);
        }
        return arr;
    }

    private org.bson.Document findActiveSessionForStudent(String studentId) {
        java.util.List<String> enrolledClassCodes = new java.util.ArrayList<>();
        com.mongodb.client.MongoCollection<org.bson.Document> enrollments =
                database.DatabaseHelper.getInstance().getEnrollmentsCollection();
        for (org.bson.Document enrollment : enrollments.find(com.mongodb.client.model.Filters.eq("student_id", studentId))) {
            String classCode = enrollment.getString("class_code");
            if (classCode != null && !classCode.isBlank()) {
                enrolledClassCodes.add(classCode);
            }
        }
        if (enrolledClassCodes.isEmpty()) {
            return null;
        }

        long nowMillis = System.currentTimeMillis();
        com.mongodb.client.MongoCollection<org.bson.Document> sessions =
                database.DatabaseHelper.getInstance().getSessionsCollection();
        for (org.bson.Document session : sessions.find(com.mongodb.client.model.Filters.and(
                com.mongodb.client.model.Filters.eq("status", "OPEN"),
                com.mongodb.client.model.Filters.in("class_name", enrolledClassCodes)))) {
            Long endTime = documentLongOrNull(session, "end_time");
            if (endTime != null && endTime > nowMillis) {
                return session;
            }
        }
        return null;
    }

    private org.json.JSONObject sessionToJson(org.bson.Document session) {
        org.json.JSONObject obj = new org.json.JSONObject();
        obj.put("id", documentId(session));
        obj.put("subject", safe(session.getString("subject")));
        obj.put("className", safe(session.getString("class_name")));
        obj.put("room", safe(session.getString("room")));
        obj.put("status", safe(session.getString("status")));
        obj.put("startTime", documentLong(session, "start_time"));
        obj.put("endTime", documentLong(session, "end_time"));
        obj.put("serverTime", System.currentTimeMillis());
        obj.put("gpsEnabled", documentBoolean(session, "gps_enabled"));
        obj.put("wifiEnabled", documentBoolean(session, "wifi_enabled"));
        obj.put("gpsLat", safe(session.getString("gps_lat")));
        obj.put("gpsLng", safe(session.getString("gps_lng")));
        obj.put("gpsRadius", safe(session.getString("gps_radius")));
        obj.put("wifiBssid", safe(session.getString("wifi_bssid")));
        return obj;
    }

    private String aiRegisteredSubjects(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return "Khong co ma sinh vien.";
        }
        org.json.JSONArray result = new org.json.JSONArray();
        com.mongodb.client.MongoCollection<org.bson.Document> enrollments =
                database.DatabaseHelper.getInstance().getEnrollmentsCollection();
        for (org.bson.Document enrollment : enrollments.find(com.mongodb.client.model.Filters.eq("student_id", studentId))) {
            String subjectCode = firstNonBlank(enrollment.getString("subject_code"), enrollment.getString("subject_id"));
            org.bson.Document subject = findSubject(subjectCode);
            org.json.JSONObject item = new org.json.JSONObject();
            item.put("ma_mon", safe(subjectCode));
            item.put("ten_mon", subject != null ? safe(subject.getString("name")) : safe(enrollment.getString("subject_name")));
            item.put("lop", safe(enrollment.getString("class_code")));
            item.put("tin_chi", subject != null ? subject.getInteger("credits", 0) : 0);
            result.put(item);
        }
        return result.length() == 0 ? "Sinh vien khong dang ky mon hoc nao." : result.toString();
    }

    private String aiStudentSchedule(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return "Khong co ma sinh vien.";
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        java.util.List<java.util.Map<String, Object>> schedules =
                database.ScheduleRepository.getInstance().findStudentSchedulesInRange(studentId, today.minusWeeks(2), today.plusWeeks(10));
        return schedules.isEmpty() ? "Khong co lich hoc nao." : scheduleArray(schedules).toString();
    }

    private String aiStudentAttendanceSummary(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return "Khong co ma sinh vien.";
        }
        java.util.List<model.Attendance> records = new service.AttendanceService().getAttendanceHistory(studentId);
        java.util.Map<String, int[]> bySubject = new java.util.TreeMap<>();
        for (model.Attendance attendance : records) {
            String subject = firstNonBlank(attendance.getSubjectName(), "Unknown");
            int[] counts = bySubject.computeIfAbsent(subject, k -> new int[]{0, 0, 0, 0});
            if ("EXCUSED_ABSENT".equals(attendance.getStatus())) {
                continue;
            }
            counts[0]++;
            if ("PRESENT".equals(attendance.getStatus())) counts[1]++;
            else if ("ABSENT".equals(attendance.getStatus()) || "UNEXCUSED_ABSENT".equals(attendance.getStatus())) counts[2]++;
            else if ("LATE".equals(attendance.getStatus())) counts[3]++;
        }
        org.json.JSONArray result = new org.json.JSONArray();
        for (java.util.Map.Entry<String, int[]> entry : bySubject.entrySet()) {
            int[] counts = entry.getValue();
            org.json.JSONObject item = new org.json.JSONObject();
            item.put("mon_hoc", entry.getKey());
            item.put("tong_buoi", counts[0]);
            item.put("co_mat", counts[1]);
            item.put("vang", counts[2]);
            item.put("di_muon", counts[3]);
            item.put("ty_le", counts[0] > 0 ? (double) counts[1] / counts[0] * 100 : 0);
            result.put(item);
        }
        return result.length() == 0 ? "Sinh vien chua co du lieu diem danh." : result.toString();
    }

    private String aiLecturerSchedule(String lecturerName) {
        if (lecturerName == null || lecturerName.isBlank()) {
            return "Khong co ten giang vien.";
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate start = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        java.time.LocalDate end = start.plusDays(6);
        java.util.List<java.util.Map<String, Object>> schedules =
                database.ScheduleRepository.getInstance().findLecturerSchedulesInRange(lecturerName, start, end);
        return schedules.isEmpty()
                ? "Tuan nay giang vien " + lecturerName + " khong co lich day nao."
                : scheduleArray(schedules).toString();
    }

    private org.bson.Document findSubject(String subjectCode) {
        if (subjectCode == null || subjectCode.isBlank()) {
            return null;
        }
        com.mongodb.client.MongoCollection<org.bson.Document> subjects =
                database.DatabaseHelper.getInstance().getSubjectsCollection();
        org.bson.Document subject = subjects.find(com.mongodb.client.model.Filters.eq("code", subjectCode)).first();
        if (subject == null) {
            subject = subjects.find(com.mongodb.client.model.Filters.eq("_id", subjectCode)).first();
        }
        if (subject == null && org.bson.types.ObjectId.isValid(subjectCode)) {
            subject = subjects.find(com.mongodb.client.model.Filters.eq("_id", new org.bson.types.ObjectId(subjectCode))).first();
        }
        return subject;
    }

    private String extractClassCode(String classSelection) {
        if (classSelection == null) return null;
        String trimmed = classSelection.trim();
        int sep = trimmed.indexOf(" - ");
        return sep >= 0 ? trimmed.substring(0, sep).trim() : trimmed;
    }

    private String sessionDate(org.bson.Document doc) {
        String date = doc.getString("date");
        if (date != null && !date.isBlank()) return date;
        Long start = documentLongOrNull(doc, "start_time");
        if (start == null || start <= 0) return "N/A";
        return java.time.Instant.ofEpochMilli(start)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String sessionTime(org.bson.Document doc, String key) {
        Object value = doc.get(key);
        if (value instanceof Number) {
            return formatTime(((Number) value).longValue());
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return formatTime(Long.parseLong(text));
            } catch (NumberFormatException ignored) {
                return text;
            }
        }
        return "N/A";
    }

    private String formatTime(long millis) {
        if (millis <= 0) return "N/A";
        return java.time.Instant.ofEpochMilli(millis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalTime()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String formatPeriods(String periods) {
        if (periods == null || periods.isBlank()) return "N/A";
        try {
            String[] parts = periods.split(",");
            int startP = Integer.parseInt(parts[0].trim());
            int endP = Integer.parseInt(parts[parts.length - 1].trim());
            return getPeriodTime(startP, true) + " - " + getPeriodTime(endP, false);
        } catch (Exception e) {
            return "N/A";
        }
    }

    private String getPeriodTime(int period, boolean isStart) {
        String[] startTimes = {"07:30", "08:20", "09:10", "10:00", "10:50", "13:00", "13:50", "14:40", "15:40", "16:30"};
        String[] endTimes = {"08:20", "09:10", "10:00", "10:50", "11:40", "13:50", "14:40", "15:30", "16:30", "17:20"};
        if (period < 1 || period > 10) return "00:00";
        return isStart ? startTimes[period - 1] : endTimes[period - 1];
    }

    private String documentId(org.bson.Document doc) {
        Object id = doc.get("_id");
        if (id instanceof org.bson.types.ObjectId objectId) {
            return objectId.toHexString();
        }
        return id != null ? id.toString() : "";
    }

    private long documentLong(org.bson.Document doc, String key) {
        Long value = documentLongOrNull(doc, key);
        return value != null ? value : 0L;
    }

    private Long documentLongOrNull(org.bson.Document doc, String key) {
        Object value = doc.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof java.util.Date date) {
            return date.getTime();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private boolean documentBoolean(org.bson.Document doc, String key) {
        Object value = doc.get(key);
        if (value instanceof Boolean bool) return bool;
        if (value instanceof String text) return Boolean.parseBoolean(text);
        return false;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String safe(Object value) {
        return value != null ? value.toString() : "";
    }

    private void close() {
        try {
            if (reader != null)
                reader.close();
            if (writer != null)
                writer.close();
            if (socket != null)
                socket.close();
            // Unregister writer when connection closes
            if (this.userId != null) {
                BroadcastManager.getInstance().unregisterUserWriter(this.userId);
            }
            for (String room : joinedRooms) {
                BroadcastManager.getInstance().unregisterClientFromRoom(room, this);
            }
        } catch (IOException e) {
            System.err.println("[Handler] Loi dong: " + e.getMessage());
        }
    }

    private Response handleSubmitAttendance(Request req) {
        if (userId == null) {
            return Response.error("Chưa đăng nhập.");
        }

        String sessionId = req.getPayloadValue("session_id");
        String deviceId = req.getPayloadValue("device_id");
        String method = req.getPayloadValue("method");

        if (sessionId == null || sessionId.isEmpty()) return Response.error("Thiếu session_id");
        if (deviceId == null || deviceId.isEmpty()) return Response.error("Thiếu device_id");

        try {
            org.bson.Document session = database.SessionRepository.getInstance().findById(sessionId);
            if (session == null || !"OPEN".equals(session.getString("status"))) {
                return Response.error("Phiên điểm danh không tồn tại hoặc đã đóng.");
            }

            // Kiem tra thoi gian de set trang thai
            long now = System.currentTimeMillis();
            long startTime = session.getLong("start_time");
            String attendanceStudentId = firstNonBlank(authenticatedStudentId, userId);
            // Cho phép đi muộn trong 1/3 thời gian đầu, hoặc tuỳ logic. Ở đây ta coi nộp thành công là PRESENT.
            // Nếu gửi sau khi quá hạn thì error ở trên đã bắt.
            
            org.bson.Document attendanceDoc = new org.bson.Document();
            attendanceDoc.put("session_id", sessionId);
            attendanceDoc.put("student_id", attendanceStudentId);
            attendanceDoc.put("user_id", userId);
            attendanceDoc.put("student_name", this.userName);
            attendanceDoc.put("device_id", deviceId);
            attendanceDoc.put("method", method);
            attendanceDoc.put("status", "PRESENT");
            attendanceDoc.put("timestamp", now);
            
            database.AttendanceRepository repo = new database.AttendanceRepository();
            if (repo.findBySessionAndStudent(sessionId, attendanceStudentId) != null) {
                return Response.error("Bạn đã điểm danh trong phiên này rồi.");
            }
            repo.insert(attendanceDoc);
            
            return Response.ok("Điểm danh thành công!");
        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("device_id")) {
                return Response.error("Thiết bị này đã được sử dụng để điểm danh cho người khác trong phiên này!");
            }
            return Response.error("Bạn đã điểm danh trong phiên này rồi.");
        } catch (Exception e) {
            return Response.error("Lỗi server khi lưu điểm danh: " + e.getMessage());
        }
    }

    /**
     * Gửi message trực tiếp đến client (thread-safe)
     */
    public synchronized void sendMessage(String json) {
        if (writer != null) {
            writer.println(json);
        }
    }
}

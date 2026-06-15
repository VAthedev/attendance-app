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
                case PING:
                    return Response.ok("pong");
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

        // Lưu userId để đăng ký writer cho notifications/broadcast
        this.userId = String.valueOf(user.getId());
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

        UserRepository repo = new UserRepository();
        User user = repo.findByEmail(email);
        if (user == null)
            return Response.error("Email không tồn tại trong hệ thống.");

        String otp = SHA256Util.generateOTP();
        otpStore.put(email, otp);

        try {
            service.EmailService.getInstance().sendOTP(email, otp);
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
        String password = req.getPayloadValue("password");
        if (email == null || password == null)
            return Response.error("Thieu thong tin.");

        UserRepository repo = new UserRepository();
        boolean ok = repo.updatePassword(email, password);
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

    /**
     * Gửi message trực tiếp đến client (thread-safe)
     */
    public synchronized void sendMessage(String json) {
        if (writer != null) {
            writer.println(json);
        }
    }
}

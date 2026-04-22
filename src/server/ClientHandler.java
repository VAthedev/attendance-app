package server;

import database.UserRepository;
import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import model.User;
import protocol.Request;
import protocol.Response;
import security.SHA256Util;
import security.TokenManager;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader  reader;
    private PrintWriter     writer;

    private static final Map<String, String> otpStore = new java.util.concurrent.ConcurrentHashMap<>();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),  "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String line;
            while ((line = reader.readLine()) != null) {
                Request  req = Request.fromJson(line);
                Response res = handleRequest(req);
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
                case LOGIN:           return handleLogin(req);
                case REGISTER:        return handleRegister(req);
                case FORGOT_PASSWORD: return handleForgotPassword(req);
                case VERIFY_OTP:      return handleVerifyOTP(req);
                case RESET_PASSWORD:  return handleResetPassword(req);
                case PING:            return Response.ok("pong");
                default:              return Response.error("Chua ho tro: " + req.getType());
            }
        } catch (Exception e) {
            System.err.println("[Handler] Loi xu ly: " + e.getMessage());
            return Response.error("Loi server: " + e.getMessage());
        }
    }

    private Response handleLogin(Request req) throws SQLException {
        String username = req.getPayloadValue("username");
        String password = req.getPayloadValue("password");
        if (username == null || username.isEmpty()) return Response.error("Thieu username.");
        if (password == null || password.isEmpty()) return Response.error("Thieu password.");

        UserRepository repo = new UserRepository();
        User user = repo.login(username, password);

        if (user == null) {
            return Response.error("Ten dang nhap hoac mat khau khong dung.");
        }

        String token = TokenManager.generateToken(user.getId(), user.getUsername());
        repo.saveSessionToken(user.getId(), token);

        Map<String, Object> data = new HashMap<>();
        data.put("token",    token);
        data.put("role",     user.getRole());
        data.put("userId",   user.getId());
        data.put("fullName", user.getFullName() != null ? user.getFullName() : "");
        return Response.ok(data);
    }

    private Response handleRegister(Request req) throws SQLException {
        String username  = req.getPayloadValue("username");
        String password  = req.getPayloadValue("password");
        String role      = req.getPayloadValue("role");
        String fullName  = req.getPayloadValue("fullName");
        String studentId = req.getPayloadValue("studentId");
        String email     = req.getPayloadValue("email");

        if (username == null || username.isEmpty()) return Response.error("Thieu username.");
        if (password == null || password.isEmpty()) return Response.error("Thieu password.");
        if (role == null || role.isEmpty())         return Response.error("Thieu role.");

        if (!role.equals("STUDENT") && !role.equals("LECTURER"))
            return Response.error("Role khong hop le: " + role);

        UserRepository repo = new UserRepository();
        repo.register(username, password, role, fullName, studentId, email);
        return Response.ok("Dang ky thanh cong.");
    }

    private Response handleForgotPassword(Request req) throws SQLException {
        String email = req.getPayloadValue("email");
        if (email == null) return Response.error("Thieu email.");

        UserRepository repo = new UserRepository();
        User user = repo.findByEmail(email);
        if (user == null) return Response.error("Email khong ton tai.");

        String otp = SHA256Util.generateOTP();
        otpStore.put(email, otp);
        return Response.ok("OTP da gui den " + email);
    }

    private Response handleVerifyOTP(Request req) {
        String email = req.getPayloadValue("email");
        String otp   = req.getPayloadValue("otp");
        if (email == null || otp == null) return Response.error("Thieu thong tin.");

        String stored = otpStore.get(email);
        if (stored == null)      return Response.error("OTP het han.");
        if (!stored.equals(otp)) return Response.error("OTP khong dung.");

        otpStore.remove(email);
        return Response.ok("Xac nhan thanh cong.");
    }

    private Response handleResetPassword(Request req) throws SQLException {
        String email    = req.getPayloadValue("email");
        String password = req.getPayloadValue("password");
        if (email == null || password == null) return Response.error("Thieu thong tin.");

        UserRepository repo = new UserRepository();
        boolean ok = repo.updatePassword(email, password);
        return ok ? Response.ok("Cap nhat mat khau thanh cong.")
                  : Response.error("Khong tim thay tai khoan.");
    }

    private void close() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("[Handler] Loi dong: " + e.getMessage());
        }
    }
}

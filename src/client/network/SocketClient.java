package client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import protocol.Request;
import protocol.Response;

public class SocketClient {

    private static final String HOST = "136.110.18.81";
    private static final int PORT = 9999;

    private static SocketClient instance;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connected = false;
    private String currentUserId;
    private String currentUserName;
    private String currentUserRole;
    private final Map<String, CompletableFuture<Response>> pending = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<Response>> pushListeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean listenerRunning = new AtomicBoolean(false);

    public static SocketClient getInstance() {
        if (instance == null)
            instance = new SocketClient();
        return instance;
    }

    private SocketClient() {
    }

    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);// ket noi sever port localhost:9999
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));// Nhan
                                                                                                                // reponse
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);// Gui
                                                                                                                     // request
                                                                                                                     // dang
                                                                                                                     // JSON
            connected = true;
            startListener();
            return true;
        } catch (IOException e) {
            connected = false;
            System.err.println("[Client] Khong the ket noi server: " + e.getMessage());
            return false;
        }
    }

    public Response send(Request request) {
        if (!connected) {
            if (!connect())
                return Response.error("Khong the ket noi server.");
        }
        try {
            String json = request.toJson();
            String nonce = request.getNonce();
            CompletableFuture<Response> future = new CompletableFuture<>();
            pending.put(nonce, future);
            writer.println(json);

            // Wait for response (timeout 30s)
            Response res = future.get(30, TimeUnit.SECONDS);
            return res;
        } catch (TimeoutException te) {
            return Response.error("Timeout chờ phản hồi từ server.");
        } catch (Exception e) {
            connected = false;
            return Response.error("Loi mang: " + e.getMessage());
        } finally {
            // cleanup handled by listener when it receives the response
        }
    }

    // bọc thao tác gửi nhận vào một thread riêng để tránh làm đơ giao diện người
    // dùng
    public void sendAsync(Request request, ResponseCallback callback) {
        CompletableFuture.runAsync(() -> {
            Response res = send(request);
            javafx.application.Platform.runLater(() -> callback.onResponse(res));
        });
    }

    public void disconnect() {
        connected = false;
        try {
            if (reader != null)
                reader.close();
            if (writer != null)
                writer.close();
            if (socket != null)
                socket.close();
            listenerRunning.set(false);
            currentUserId = null;
            currentUserName = null;
            currentUserRole = null;
        } catch (IOException e) {
            System.err.println("[Client] Loi ngat ket noi: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public interface ResponseCallback {
        void onResponse(Response response);
    }

    public void setCurrentUser(String userId, String userName, String role) {
        this.currentUserId = userId;
        this.currentUserName = userName;
        this.currentUserRole = role;
    }

    public String getCurrentUserId() { return currentUserId; }
    public String getCurrentUserName() { return currentUserName; }
    public String getCurrentUserRole() { return currentUserRole; }

    public String getDisplayNameOrFallback() {
        if (currentUserName != null && !currentUserName.isBlank()) return currentUserName;
        if (currentUserId != null && !currentUserId.isBlank()) return currentUserId;
        return "Tôi";
    }

    // Push listener support
    public void addPushListener(Consumer<Response> listener) { pushListeners.add(listener); }
    public void removePushListener(Consumer<Response> listener) { pushListeners.remove(listener); }

    private void startListener() {
        if (!listenerRunning.compareAndSet(false, true)) return;
        Thread t = new Thread(() -> {
            try {
                String line;
                while (listenerRunning.get() && (line = reader.readLine()) != null) {
                    Response res = Response.fromJson(line);
                    String nonce = res.getNonce();
                    if (nonce == null || nonce.isBlank()) {
                        nonce = res.getDataValue("nonce");
                    }
                    if (nonce != null && !nonce.isEmpty() && pending.containsKey(nonce)) {
                        CompletableFuture<Response> f = pending.remove(nonce);
                        if (f != null) f.complete(res);
                    } else {
                        // push message — dispatch to listeners on UI thread
                        for (Consumer<Response> l : pushListeners) {
                            javafx.application.Platform.runLater(() -> l.accept(res));
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("[Client Listener] Error: " + e.getMessage());
            } finally {
                listenerRunning.set(false);
                connected = false;
            }
        }, "SocketClient-Listener");
        t.setDaemon(true);
        t.start();
    }
}

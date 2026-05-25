package server;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import model.ChatMessage;
import protocol.Response;

/**
 * BroadcastManager - Quản lý broadcast real-time cho chat và notifications
 * 
 * Thread-safe implementation với ConcurrentHashMap + CopyOnWriteArrayList
 * Cho phép broadcast messages đến toàn bộ clients trong một session
 */
public class BroadcastManager {

    private static BroadcastManager instance;

    // Map: roomKey -> List of ClientHandlers in that room
    private final Map<String, List<ClientHandler>> roomClients = new ConcurrentHashMap<>();

    // Map: userId -> PrintWriter (để broadcast notifications)
    private final Map<String, PrintWriter> userWriters = new ConcurrentHashMap<>();

    private BroadcastManager() {
    }

    /**
     * Singleton instance
     */
    public static synchronized BroadcastManager getInstance() {
        if (instance == null) {
            instance = new BroadcastManager();
        }
        return instance;
    }

    /**
     * Đăng ký client vào session (cho chat)
     */
    public void registerClientInRoom(String roomKey, ClientHandler handler) {
        if (roomKey == null || roomKey.isBlank()) return;
        roomClients.computeIfAbsent(roomKey, k -> new CopyOnWriteArrayList<>())
                   .add(handler);
        System.out.println("[Broadcast] Client registered in room: " + roomKey +
            " (total: " + roomClients.get(roomKey).size() + ")");
    }

    /**
     * Unregister client khỏi session
     */
    public void unregisterClientFromRoom(String roomKey, ClientHandler handler) {
        List<ClientHandler> clients = roomClients.get(roomKey);
        if (clients != null) {
            clients.remove(handler);
            if (clients.isEmpty()) {
                roomClients.remove(roomKey);
                System.out.println("[Broadcast] Room cleared: " + roomKey);
            }
        }
    }

    /**
     * Đăng ký user writer cho notifications
     */
    public void registerUserWriter(String userId, PrintWriter writer) {
        userWriters.put(userId, writer);
    }

    /**
     * Unregister user writer
     */
    public void unregisterUserWriter(String userId) {
        userWriters.remove(userId);
    }

    /**
     * Broadcast chat message đến tất cả clients trong session
     * Sử dụng async để không block ClientHandler
     */
    public void broadcastChatMessage(String roomKey, ChatMessage message) {
        List<ClientHandler> clients = roomClients.get(roomKey);
        if (clients == null || clients.isEmpty()) {
            System.out.println("[Broadcast] No clients in room: " + roomKey);
            return;
        }

        // Gửi async để không block thread hiện tại
        ThreadPoolManager.getInstance().submitAsync(() -> {
            Response response = Response.ok("CHAT_MESSAGE");
            response.putPayload("type", "CHAT_MESSAGE");
            response.putPayload("message_id", message.getId());
            response.putPayload("room_key", message.getRoomKey());
            response.putPayload("room_name", message.getRoomName());
            response.putPayload("sender", message.getSender());
            response.putPayload("sender_name", message.getSenderName());
            response.putPayload("content", message.getContent());
            response.putPayload("timestamp", message.getTimestamp());

            String jsonResponse = response.toJson();

            for (ClientHandler client : clients) {
                try {
                    client.sendMessage(jsonResponse);  // Sẽ implement trong ClientHandler
                } catch (Exception e) {
                    System.err.println("[Broadcast] Error sending to client: " + e.getMessage());
                }
            }

            System.out.println("[Broadcast] Message sent to " + clients.size() + 
                " clients in room: " + roomKey);
            return null;
        });
    }

    /**
     * Broadcast notification đến user cụ thể
     */
    public void sendNotificationToUser(String userId, String title, String message) {
        PrintWriter writer = userWriters.get(userId);
        if (writer == null) {
            System.out.println("[Broadcast] User not connected: " + userId);
            return;
        }

        Response response = Response.ok("NOTIFICATION");
        response.putPayload("title", title);
        response.putPayload("message", message);
        response.putPayload("timestamp", System.currentTimeMillis());

        writer.println(response.toJson());
        System.out.println("[Broadcast] Notification sent to: " + userId);
    }

    /**
     * Broadcast notification đến nhóm users
     */
    public void broadcastNotificationToUsers(List<String> userIds, String title, String message) {
        ThreadPoolManager.getInstance().submitAsync(() -> {
            Response response = Response.ok("NOTIFICATION");
            response.putPayload("title", title);
            response.putPayload("message", message);
            response.putPayload("timestamp", System.currentTimeMillis());

            String jsonResponse = response.toJson();
            int sentCount = 0;

            for (String userId : userIds) {
                PrintWriter writer = userWriters.get(userId);
                if (writer != null) {
                    try {
                        writer.println(jsonResponse);
                        sentCount++;
                    } catch (Exception e) {
                        System.err.println("[Broadcast] Error sending to " + userId + ": " + e.getMessage());
                    }
                }
            }

            System.out.println("[Broadcast] Notification sent to " + sentCount + " / " + userIds.size() + " users");
            return null;
        });
    }

    /**
     * Broadcast announcement đến tất cả connected clients
     */
    public void broadcastAnnouncement(String message) {
        ThreadPoolManager.getInstance().submitAsync(() -> {
            Response response = Response.ok("ANNOUNCEMENT");
            response.putPayload("message", message);
            response.putPayload("timestamp", System.currentTimeMillis());

            String jsonResponse = response.toJson();
            int count = 0;

            for (PrintWriter writer : userWriters.values()) {
                try {
                    writer.println(jsonResponse);
                    count++;
                } catch (Exception e) {
                    // Ignore errors
                }
            }

            System.out.println("[Broadcast] Announcement sent to " + count + " connected users");
            return null;
        });
    }

    /**
     * Lấy số clients trong session
     */
    public int getClientCountInSession(String sessionId) {
        List<ClientHandler> clients = roomClients.get(sessionId);
        return clients != null ? clients.size() : 0;
    }

    /**
     * Lấy số users kết nối
     */
    public int getConnectedUsersCount() {
        return userWriters.size();
    }

    /**
     * Lấy thông tin toàn bộ sessions
     */
    public void printStatistics() {
        System.out.println("\n[BroadcastManager Statistics]");
        System.out.println("  Active Rooms: " + roomClients.size());
        System.out.println("  Connected Users: " + userWriters.size());
        
        int totalClients = 0;
        for (List<ClientHandler> clients : roomClients.values()) {
            totalClients += clients.size();
        }
        System.out.println("  Total Clients in Sessions: " + totalClients);
        System.out.println();
    }

    // Backward-compatible aliases
    public void registerClientInSession(String sessionId, ClientHandler handler) { registerClientInRoom(sessionId, handler); }
    public void unregisterClientFromSession(String sessionId, ClientHandler handler) { unregisterClientFromRoom(sessionId, handler); }
}

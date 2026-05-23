package service;

import java.util.UUID;

import database.ChatRepository;
import model.ChatMessage;
import server.BroadcastManager;

/**
 * ChatService - xử lý business logic chat trên server
 * Lưu ý: hiện implementation đơn giản: tạo ChatMessage và gọi BroadcastManager
 */
public class ChatService {

    private static ChatService instance;
    private final ChatRepository chatRepository;

    private ChatService() {
        this.chatRepository = new ChatRepository();
    }

    public static synchronized ChatService getInstance() {
        if (instance == null) instance = new ChatService();
        return instance;
    }

    public ChatMessage sendMessage(String roomKey, String roomName, String sender, String senderName, String content) {
        String id = UUID.randomUUID().toString();
        long ts = System.currentTimeMillis();
        ChatMessage msg = new ChatMessage(id, roomKey, roomName, sender, senderName, content, ts);

        chatRepository.saveMessage(msg);

        // Gọi BroadcastManager để gửi tới các client trong room
        BroadcastManager.getInstance().broadcastChatMessage(roomKey, msg);
        return msg;
    }

    public java.util.List<ChatMessage> loadRecentHistory(String roomKey, int days) {
        return chatRepository.findHistoryLastDays(roomKey, days);
    }

    public java.util.List<java.util.Map<String, Object>> loadRooms() {
        return chatRepository.findChatRooms();
    }
}

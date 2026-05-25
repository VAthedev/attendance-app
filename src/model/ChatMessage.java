package model;

public class ChatMessage {
    private String id;
    private String roomKey;
    private String roomName;
    private String sender;
    private String senderName;
    private String content;
    private long timestamp;

    public ChatMessage() {}

    public ChatMessage(String id, String roomKey, String roomName, String sender, String senderName, String content, long timestamp) {
        this.id = id;
        this.roomKey = roomKey;
        this.roomName = roomName;
        this.sender = sender;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getRoomKey() { return roomKey; }
    public String getRoomName() { return roomName; }
    public String getSender() { return sender; }
    public String getSenderName() { return senderName; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }

    public void setId(String id) { this.id = id; }
    public void setRoomKey(String roomKey) { this.roomKey = roomKey; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setSender(String sender) { this.sender = sender; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Backward-compatible aliases for older code
    public String getSessionId() { return roomKey; }
    public void setSessionId(String sessionId) { this.roomKey = sessionId; }
}

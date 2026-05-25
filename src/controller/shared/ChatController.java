package controller.shared;

import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import client.network.SocketClient;
import database.ChatRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import protocol.Request;
import protocol.RequestType;
import protocol.Response;

public class ChatController implements Initializable {

    @FXML private Label lblRoomTitle;
    @FXML private Label lblRoomSubtitle;
    @FXML private Label lblCurrentRoom;
    @FXML private Label lblStatus;
    @FXML private Label lblMiniUserName;
    @FXML private ComboBox<String> cbRooms;
    @FXML private VBox messageListBox;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField messageField;
    @FXML private Button sendButton;

    private final SocketClient client = SocketClient.getInstance();
    private final ChatRepository chatRepository = new ChatRepository();
    private final Consumer<Response> pushListener = this::handlePush;
    private final ObservableList<RoomOption> roomOptions = FXCollections.observableArrayList();

    private String activeRoomKey;
    private String activeRoomName;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!client.isConnected()) {
            client.connect();
        }
        client.addPushListener(pushListener);

        if (messageListBox != null) {
            addSystemMessage("Chọn một môn học để vào phòng chat. Lịch sử 30 ngày gần nhất sẽ được nạp tự động.");
        }

        if (lblMiniUserName != null) {
            lblMiniUserName.setText(client.getDisplayNameOrFallback());
        }

        loadRooms();
        cbRooms.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            RoomOption selected = findRoomByDisplay(newValue);
            if (selected != null) {
                switchRoom(selected);
            }
        });
    }

    private void loadRooms() {
        try {
            List<Map<String, Object>> rooms = chatRepository.findChatRooms();
            roomOptions.clear();
            if (rooms != null) {
                for (Map<String, Object> room : rooms) {
                    String roomKey = value(room.get("roomKey"), "");
                    String roomName = value(room.get("roomName"), roomKey);
                    if (!roomKey.isBlank()) {
                        roomOptions.add(new RoomOption(roomKey, roomName));
                    }
                }
            }

            if (roomOptions.isEmpty()) {
                roomOptions.add(new RoomOption("GENERAL", "Chat chung"));
            }

            cbRooms.setItems(FXCollections.observableArrayList(
                roomOptions.stream().map(RoomOption::displayText).toList()
            ));
            cbRooms.getSelectionModel().selectFirst();
            switchRoom(roomOptions.get(0));
        } catch (Exception e) {
            setStatus("Không tải được danh sách phòng chat: " + e.getMessage());
            roomOptions.clear();
            roomOptions.add(new RoomOption("GENERAL", "Chat chung"));
            cbRooms.setItems(FXCollections.observableArrayList(roomOptions.get(0).displayText()));
            cbRooms.getSelectionModel().selectFirst();
            switchRoom(roomOptions.get(0));
        }
    }

    private void switchRoom(RoomOption room) {
        this.activeRoomKey = room.roomKey;
        this.activeRoomName = room.roomName;
        if (lblMiniUserName != null) {
            lblMiniUserName.setText(client.getDisplayNameOrFallback());
        }
        if (lblRoomTitle != null) {
            lblRoomTitle.setText(room.roomName);
        }
        if (lblCurrentRoom != null) {
            lblCurrentRoom.setText(room.roomKey);
        }
        if (lblRoomSubtitle != null) {
            lblRoomSubtitle.setText("Phòng môn học: " + room.roomKey + " • lịch sử 30 ngày gần nhất");
        }
        setStatus("Đang tải lịch sử...");
        requestHistory(room.roomKey, room.roomName);
    }

    private void requestHistory(String roomKey, String roomName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("roomKey", roomKey);
        payload.put("roomName", roomName);

        Request req = new Request(RequestType.GET_CHAT_HISTORY, payload);
        client.sendAsync(req, res -> {
            if (!res.isOk()) {
                appendLine("[Lỗi tải lịch sử] " + res.getMessage());
                return;
            }

            String history = res.getDataValue("history");
            Platform.runLater(() -> {
                messageListBox.getChildren().clear();
                if (history == null || history.isBlank()) {
                    addSystemMessage("Chưa có tin nhắn nào trong 30 ngày gần đây. Hãy mở đầu cuộc trò chuyện.");
                } else {
                    String normalizedHistory = history.replace("\\n", "\n");
                    for (String line : normalizedHistory.split("\\n")) {
                        if (!line.isBlank()) {
                            appendHistoryLine(line);
                        }
                    }
                }
                setStatus("Đã tải " + value(res.getDataValue("count"), "0") + " tin nhắn");
            });
        });
    }

    @FXML
    public void onSend() {
        String text = messageField.getText();
        if (text == null || text.isBlank() || activeRoomKey == null) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("roomKey", activeRoomKey);
        payload.put("roomName", activeRoomName);
        payload.put("sender", value(client.getCurrentUserId(), client.getDisplayNameOrFallback()));
        payload.put("senderName", client.getDisplayNameOrFallback());
        payload.put("content", text.trim());

        Request req = new Request(RequestType.SEND_CHAT, payload);
        client.sendAsync(req, res -> {
            if (!res.isOk()) {
                appendLine("[Lỗi] " + res.getMessage());
            } else {
                messageField.clear();
            }
        });
    }

    @FXML
    public void onRefreshHistory() {
        if (activeRoomKey == null) return;
        requestHistory(activeRoomKey, activeRoomName);
    }

    private void handlePush(Response res) {
        String type = res.getDataValue("type");
        if (!"CHAT_MESSAGE".equals(type)) {
            return;
        }

        String roomKey = res.getDataValue("room_key");
        if (roomKey == null || !roomKey.equals(activeRoomKey)) {
            return;
        }

        String senderName = res.getDataValue("sender_name");
        String content = res.getDataValue("content");
        long timestamp = parseLong(res.getDataValue("timestamp"));
        appendMessage(senderName, content, timestamp, true, true);
    }

    private void appendHistoryLine(String line) {
        String[] parts = line.split("\\|", 3);
        if (parts.length < 3) return;
        long timestamp = parseLong(parts[0]);
        String sender = parts[1].replace("\\|", "|");
        String content = parts[2].replace("\\|", "|");
        boolean isMine = sender.equals(client.getDisplayNameOrFallback()) || sender.equals(client.getCurrentUserId());
        appendMessage(sender, content, timestamp, isMine, false);
    }

    private void appendMessage(String sender, String content, long timestamp, boolean isMine, boolean realtime) {
        if (messageListBox == null) return;

        VBox bubble = createBubble(sender, content, timestamp, isMine, realtime);
        HBox row = new HBox();
        row.setFillHeight(false);
        row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 0, 0));

        if (isMine) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            row.getChildren().addAll(spacer, bubble);
        } else {
            row.getChildren().add(bubble);
        }

        messageListBox.getChildren().add(row);
        scrollToBottom();
    }

    private void addSystemMessage(String message) {
        if (messageListBox == null) return;
        Label label = new Label(message);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-background-color: #f8fafc; -fx-background-radius: 12; -fx-padding: 8 12 8 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12;");
        HBox row = new HBox(label);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(4, 0, 4, 0));
        messageListBox.getChildren().add(row);
        scrollToBottom();
    }

    private VBox createBubble(String sender, String content, long timestamp, boolean isMine, boolean realtime) {
        VBox card = new VBox(4);
        card.setMaxWidth(520);
        card.setStyle("-fx-padding: 0;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label(isMine ? "🙂" : "👤");
        avatar.setMinSize(30, 30);
        avatar.setPrefSize(30, 30);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color: " + (isMine ? "#dbeafe" : "#eef2ff") + "; -fx-background-radius: 999; -fx-font-size: 14px;");

        VBox nameBox = new VBox(1);
        Label name = new Label(sender != null && !sender.isBlank() ? sender : "Ẩn danh");
        name.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        Label time = new Label(formatTimestamp(timestamp) + (realtime ? " • realtime" : ""));
        time.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        nameBox.getChildren().addAll(name, time);

        if (isMine) {
            header.setAlignment(Pos.CENTER_RIGHT);
            header.getChildren().addAll(nameBox, avatar);
        } else {
            header.getChildren().addAll(avatar, nameBox);
        }

        Label body = new Label(content != null ? content : "");
        body.setWrapText(true);
        body.setMaxWidth(420);
        body.setStyle("-fx-font-size: 13px; -fx-text-fill: #0f172a; -fx-padding: 10 12 10 12; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-width: 1; " +
                (isMine
                        ? "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-border-color: #2563eb;"
                        : "-fx-background-color: #ffffff; -fx-border-color: #dbe4f0;"));

        if (isMine) {
            body.setStyle("-fx-font-size: 13px; -fx-text-fill: white; -fx-padding: 10 12 10 12; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-width: 1; -fx-background-color: #2563eb; -fx-border-color: #2563eb;");
        }

        card.getChildren().addAll(header, body);
        return card;
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp <= 0) return "";
        return DateTimeFormatter.ofPattern("HH:mm dd/MM")
                .format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()));
    }

    private void scrollToBottom() {
        if (chatScrollPane == null || chatScrollPane.getContent() == null) return;
        Platform.runLater(() -> {
            chatScrollPane.layout();
            chatScrollPane.setVvalue(1.0);
        });
    }

    private void appendLine(String line) {
        Platform.runLater(() -> addSystemMessage(line));
    }

    private void setStatus(String text) {
        Platform.runLater(() -> {
            if (lblStatus != null) {
                lblStatus.setText(text);
            }
        });
    }

    public void dispose() {
        client.removePushListener(pushListener);
    }

    private RoomOption findRoomByDisplay(String displayText) {
        for (RoomOption option : roomOptions) {
            if (option.displayText().equals(displayText)) {
                return option;
            }
        }
        return null;
    }

    private String value(Object obj, String fallback) {
        return obj != null ? obj.toString() : fallback;
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static class RoomOption {
        private final String roomKey;
        private final String roomName;

        private RoomOption(String roomKey, String roomName) {
            this.roomKey = roomKey;
            this.roomName = roomName;
        }

        private String displayText() {
            return roomName + " (" + roomKey + ")";
        }
    }
}

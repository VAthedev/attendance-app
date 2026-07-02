package controller;

import ai.VirtualAssistant;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.ListCell;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ChatbotController {

    @FXML private ListView<Message> chatListView;
    @FXML private TextField inputField;
    @FXML private Button sendButton;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;

    private VirtualAssistant virtualAssistant;
    
    // Giả sử có 1 biến lưu ID người dùng hiện tại (bạn cần truyền vào từ màn hình chính)
    private String currentUserId = ""; 

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }

    @FXML
    public void initialize() {
        virtualAssistant = new VirtualAssistant();
        
        // Lấy ID tự động từ StudentDashboard hoặc LecturerDashboard
        this.currentUserId = controller.student.StudentDashboardController.currentStudentId;
        if (this.currentUserId == null || this.currentUserId.isEmpty()) {
            String gvId = controller.lecturer.LecturerDashboardController.currentLecturerId;
            String gvName = controller.lecturer.LecturerDashboardController.currentLecturerName;
            if (gvId != null && !gvId.isEmpty()) {
                this.currentUserId = "GV: " + gvName + " (" + gvId + ")";
            }
        }
        if (this.currentUserId == null || this.currentUserId.isEmpty()) {
            this.currentUserId = "USER_TEST";
        }
        
        chatListView.setCellFactory(param -> new ChatCell());
        
        // Lời chào ban đầu
        addMessage(new Message("Xin chào! Mình là AI hỗ trợ học vụ. Mình có thể giúp gì cho bạn hôm nay?", false));
    }

    @FXML
    private void handleSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        inputField.clear();
        addMessage(new Message(text, true));
        
        setLoading(true);

        // Chạy ngầm gọi API Gemini để không làm đơ giao diện
        new Thread(() -> {
            String response = virtualAssistant.ask(text, currentUserId);
            
            Platform.runLater(() -> {
                addMessage(new Message(response, false));
                setLoading(false);
            });
        }).start();
    }

    private void addMessage(Message msg) {
        chatListView.getItems().add(msg);
        chatListView.scrollTo(chatListView.getItems().size() - 1);
    }
    
    private void setLoading(boolean isLoading) {
        loadingIndicator.setVisible(isLoading);
        statusLabel.setVisible(isLoading);
        sendButton.setDisable(isLoading);
        inputField.setDisable(isLoading);
    }

    // --- Inner classes để hiển thị tin nhắn (Right/Left) ---
    private static class Message {
        String text;
        boolean isUser;
        public Message(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }
    }

    private static class ChatCell extends ListCell<Message> {
        @Override
        protected void updateItem(Message item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                setStyle("-fx-background-color: transparent;");
            } else {
                HBox container = new HBox();
                
                Label textNode = new Label(item.text);
                textNode.setWrapText(true);
                textNode.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
                
                // Tránh scroll ngang bằng cách giới hạn chiều rộng của tin nhắn (75% ListView)
                if (getListView() != null) {
                    textNode.maxWidthProperty().bind(getListView().widthProperty().multiply(0.75));
                } else {
                    textNode.setMaxWidth(280);
                }
                
                textNode.setPadding(new javafx.geometry.Insets(10, 15, 10, 15));
                textNode.setFont(javafx.scene.text.Font.font("Segoe UI", 14));
                
                if (item.isUser) {
                    textNode.setStyle("-fx-background-color: #0084FF; -fx-background-radius: 20; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
                    container.setAlignment(Pos.CENTER_RIGHT);
                    HBox.setMargin(textNode, new javafx.geometry.Insets(5, 15, 5, 0));
                } else {
                    textNode.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 20; -fx-text-fill: #333333; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
                    container.setAlignment(Pos.CENTER_LEFT);
                    HBox.setMargin(textNode, new javafx.geometry.Insets(5, 0, 5, 15));
                }
                
                container.getChildren().add(textNode);
                setGraphic(container);
                setStyle("-fx-background-color: transparent; -fx-padding: 2px;");
            }
        }
    }
}

package controller.student;

import client.network.ServerApi;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import model.Notification;
import protocol.RequestType;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class NotificationController implements Initializable {

    @FXML private VBox vboxNotifications;
    @FXML private VBox boxLoading;
    @FXML private VBox boxEmpty;
    @FXML private Button btnMarkAllRead;

    private String currentStudentId;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentStudentId = StudentDashboardController.currentStudentId;

        if (currentStudentId != null) {
            loadNotifications();
        } else {
            showError("Không xác định được sinh viên đăng nhập.");
        }
    }

    private void loadNotifications() {
        showLoading(true);

        new Thread(() -> {
            try {
                protocol.Response res = ServerApi.send(RequestType.GET_NOTIFICATIONS,
                        java.util.Map.of("studentId", currentStudentId));
                if (!res.isOk()) {
                    throw new IllegalStateException(res.getMessage());
                }
                List<Notification> notifications = parseNotifications(ServerApi.getArray(res, "notifications"));

                Platform.runLater(() -> {
                    showLoading(false);
                    displayNotifications(notifications);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showLoading(false);
                    showError("Lỗi khi tải thông báo: " + e.getMessage());
                });
            }
        }).start();
    }

    private void displayNotifications(List<Notification> notifications) {
        vboxNotifications.getChildren().clear();

        if (notifications == null || notifications.isEmpty()) {
            boxEmpty.setVisible(true);
            boxEmpty.setManaged(true);
            btnMarkAllRead.setDisable(true);
            return;
        }

        boxEmpty.setVisible(false);
        boxEmpty.setManaged(false);
        
        boolean hasUnread = false;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Notification notif : notifications) {
            HBox card = new HBox(15);
            card.getStyleClass().add("dashboard-card");
            card.setPadding(new Insets(15, 20, 15, 20));
            card.setAlignment(Pos.CENTER_LEFT);

            if (!notif.isRead()) {
                card.setStyle("-fx-border-color: #3498db; -fx-border-width: 0 0 0 4px; -fx-background-color: #f0f8ff;");
                hasUnread = true;
            }

            // Icon based on type
            Label icon = new Label();
            icon.setStyle("-fx-font-size: 24px;");
            String color = "#34495e";
            switch (notif.getType()) {
                case "SUCCESS": icon.setText("✔"); color = "#2ecc71"; break;
                case "INFO": icon.setText("i"); color = "#3498db"; break;
                case "ALERT": icon.setText("⚠"); color = "#e74c3c"; break;
                default: icon.setText("•"); color = "#95a5a6"; break;
            }
            VBox content = new VBox(5);
            HBox.setHgrow(content, Priority.ALWAYS);

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            Label title = new Label(notif.getTitle());
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + color + ";");
            
            if (!notif.isRead()) {
                Circle dot = new Circle(4, javafx.scene.paint.Color.web("#3498db"));
                header.getChildren().addAll(dot, title);
            } else {
                header.getChildren().add(title);
            }

            Label message = new Label(notif.getMessage());
            message.setWrapText(true);
            message.setStyle("-fx-text-fill: #34495e; -fx-font-size: 13px;");

            Label time = new Label(notif.getCreatedAt() != null ? notif.getCreatedAt().format(dtf) : "");
            time.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");

            content.getChildren().addAll(header, message, time);

            // Action
            VBox actionBox = new VBox();
            actionBox.setAlignment(Pos.CENTER);
            if (!notif.isRead() && notif.getId() != null) {
                Button btnRead = new Button("Đánh dấu đã đọc");
                btnRead.getStyleClass().add("btn-outline");
                btnRead.setStyle("-fx-font-size: 11px; -fx-padding: 4 8 4 8;");
                btnRead.setOnAction(e -> {
                    markAsRead(notif.getId());
                });
                actionBox.getChildren().add(btnRead);
            }

            card.getChildren().addAll(icon, content, actionBox);
            vboxNotifications.getChildren().add(card);
        }

        btnMarkAllRead.setDisable(!hasUnread);
    }

    private List<Notification> parseNotifications(org.json.JSONArray arr) {
        java.util.ArrayList<Notification> notifications = new java.util.ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            org.json.JSONObject obj = arr.getJSONObject(i);
            Notification notif = new Notification();
            notif.setId(obj.optString("id", null));
            notif.setStudentId(obj.optString("studentId", ""));
            notif.setTitle(obj.optString("title", ""));
            notif.setMessage(obj.optString("message", ""));
            notif.setType(obj.optString("type", "INFO"));
            notif.setRead(obj.optBoolean("isRead", false));
            String createdAt = obj.optString("createdAt", "");
            if (!createdAt.isBlank()) {
                try {
                    notif.setCreatedAt(LocalDateTime.parse(createdAt));
                } catch (Exception ignored) {}
            }
            notifications.add(notif);
        }
        return notifications;
    }

    private void markAsRead(String id) {
        new Thread(() -> {
            try {
                protocol.Response res = ServerApi.send(RequestType.MARK_NOTIFICATION_READ,
                        java.util.Map.of("notificationId", id));
                if (!res.isOk()) {
                    throw new IllegalStateException(res.getMessage());
                }
                Platform.runLater(this::loadNotifications);
            } catch (Exception e) {
                Platform.runLater(() -> showError("Lỗi khi cập nhật trạng thái: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleMarkAllRead() {
        if (currentStudentId == null) return;
        
        new Thread(() -> {
            try {
                protocol.Response res = ServerApi.send(RequestType.MARK_NOTIFICATION_READ,
                        java.util.Map.of("studentId", currentStudentId, "all", "true"));
                if (!res.isOk()) {
                    throw new IllegalStateException(res.getMessage());
                }
                Platform.runLater(this::loadNotifications);
            } catch (Exception e) {
                Platform.runLater(() -> showError("Lỗi khi cập nhật tất cả: " + e.getMessage()));
            }
        }).start();
    }

    private void showLoading(boolean loading) {
        boxLoading.setVisible(loading);
        boxLoading.setManaged(loading);
        vboxNotifications.setVisible(!loading);
        vboxNotifications.setManaged(!loading);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

package controller.widgets;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class StudentNotificationsWidgetController {

    @FXML private VBox boxNotifications;
    @FXML private Label lblNoNotif;

    public void loadData(String studentId) {
        if (studentId == null || studentId.isEmpty()) return;
        new Thread(() -> {
            try {
                service.NotificationService notifService = new service.NotificationService();
                java.util.List<model.Notification> notifications = notifService.getNotificationsForStudent(studentId);
                
                Platform.runLater(() -> {
                    boxNotifications.getChildren().clear();
                    if (notifications == null || notifications.isEmpty()) {
                        lblNoNotif.setVisible(true);
                        lblNoNotif.setManaged(true);
                    } else {
                        lblNoNotif.setVisible(false);
                        lblNoNotif.setManaged(false);
                        int count = Math.min(3, notifications.size());
                        for (int i = 0; i < count; i++) {
                            model.Notification n = notifications.get(i);
                            String prefix = n.isRead() ? "" : "🔵 ";
                            addNotificationItem(prefix + n.getTitle(), n.getMessage());
                        }
                    }
                });
            } catch(Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void addNotificationItem(String title, String message) {
        VBox item = new VBox(4);
        item.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:8;" +
                      "-fx-padding:10 14 10 14;-fx-border-color:#e2e8f0;" +
                      "-fx-border-radius:8;-fx-border-width:1;");
        Label t = new Label(title);   t.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1a2744;");
        Label m = new Label(message); m.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7a99;");
        item.getChildren().addAll(t, m);
        boxNotifications.getChildren().add(item);
    }
    
    @FXML
    private void handleShowAll() {
        // Có thể thực thi việc chuyển màn qua event bus hoặc tham chiếu controller
    }
}

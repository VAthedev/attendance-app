package controller.widgets;

import client.network.ServerApi;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import protocol.RequestType;

public class StudentNotificationsWidgetController {

    @FXML private VBox boxNotifications;
    @FXML private Label lblNoNotif;

    public void loadData(String studentId) {
        if (studentId == null || studentId.isEmpty()) return;
        new Thread(() -> {
            try {
                protocol.Response res = ServerApi.send(RequestType.GET_NOTIFICATIONS,
                        java.util.Map.of("studentId", studentId));
                if (!res.isOk()) {
                    throw new IllegalStateException(res.getMessage());
                }
                org.json.JSONArray notifications = ServerApi.getArray(res, "notifications");

                Platform.runLater(() -> {
                    boxNotifications.getChildren().clear();
                    if (notifications.length() == 0) {
                        lblNoNotif.setVisible(true);
                        lblNoNotif.setManaged(true);
                    } else {
                        lblNoNotif.setVisible(false);
                        lblNoNotif.setManaged(false);
                        int count = Math.min(3, notifications.length());
                        for (int i = 0; i < count; i++) {
                            org.json.JSONObject n = notifications.getJSONObject(i);
                            String prefix = n.optBoolean("isRead", false) ? "" : "* ";
                            addNotificationItem(prefix + n.optString("title", ""), n.optString("message", ""));
                        }
                    }
                });
            } catch(Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void addNotificationItem(String title, String message) {
        VBox item = new VBox(4);
        item.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:8;" +
                      "-fx-padding:10 14 10 14;-fx-border-color:#e2e8f0;" +
                      "-fx-border-radius:8;-fx-border-width:1;");
        Label t = new Label(title);
        t.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1a2744;");
        Label m = new Label(message);
        m.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7a99;");
        item.getChildren().addAll(t, m);
        boxNotifications.getChildren().add(item);
    }

    @FXML
    private void handleShowAll() {
        // Navigation can be added by parent dashboard.
    }
}

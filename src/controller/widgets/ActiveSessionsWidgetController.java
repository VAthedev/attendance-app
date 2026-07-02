package controller.widgets;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ActiveSessionsWidgetController {

    @FXML private VBox boxActiveSessions;
    @FXML private Label lblNoSession;

    public void loadData(String lecturerId) {
        if (lecturerId == null || lecturerId.isEmpty()) return;

        new Thread(() -> {
            try {
                com.mongodb.client.MongoCollection<org.bson.Document> enrollmentsCol = database.DatabaseHelper.getInstance().getEnrollmentsCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> attendanceCol = database.DatabaseHelper.getInstance().getAttendanceCollection();

                List<org.bson.Document> activeSessions = database.SessionRepository.getInstance().findActiveSessions(lecturerId);
                final List<Map<String,Object>> sessionItemsData = new java.util.ArrayList<>();
                
                for (org.bson.Document session : activeSessions) {
                    String subject = session.getString("subject");
                    long startMillis = session.getLong("start_time");
                    String openTime = java.time.LocalTime.ofInstant(java.time.Instant.ofEpochMilli(startMillis), java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"));
                    String sessionId = session.getString("_id");
                    if (sessionId == null && session.getObjectId("_id") != null) sessionId = session.getObjectId("_id").toHexString();

                    long checkedInCount = attendanceCol.countDocuments(com.mongodb.client.model.Filters.eq("session_id", sessionId));
                    
                    String className = session.getString("class_name");
                    long totalCount = enrollmentsCol.countDocuments(com.mongodb.client.model.Filters.eq("subject_id", className));

                    Map<String,Object> item = new java.util.HashMap<>();
                    item.put("subject", subject);
                    item.put("status", "Đang mở");
                    item.put("openTime", openTime);
                    item.put("checkedIn", (int)checkedInCount);
                    item.put("total", (int)totalCount);
                    sessionItemsData.add(item);
                }

                Platform.runLater(() -> {
                    boxActiveSessions.getChildren().clear();
                    if (sessionItemsData.isEmpty()) {
                        lblNoSession.setVisible(true); lblNoSession.setManaged(true);
                    } else {
                        lblNoSession.setVisible(false); lblNoSession.setManaged(false);
                        for (Map<String,Object> m : sessionItemsData) {
                            addActiveSession((String)m.get("subject"), (String)m.get("status"), (String)m.get("openTime"), (int)m.get("checkedIn"), (int)m.get("total"));
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void addActiveSession(String subject, String status, String openTime,
                                   int checkedIn, int total) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color:#f0fdf4;-fx-background-radius:8;" +
                      "-fx-border-color:#86efac;-fx-border-radius:8;-fx-border-width:1;" +
                      "-fx-padding:12 14 12 14;");
        HBox top = new HBox(8);
        Label lblSubject = new Label(subject);
        lblSubject.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#14532d;");
        HBox.setHgrow(lblSubject, Priority.ALWAYS);
        Label lblStatus = new Label("● " + status);
        lblStatus.setStyle("-fx-text-fill:#16a34a;-fx-font-size:12px;-fx-font-weight:bold;");
        top.getChildren().addAll(lblSubject, lblStatus);
        Label lblInfo = new Label("Mở lúc " + openTime + "  •  " + checkedIn + "/" + total + " SV đã điểm danh");
        lblInfo.setStyle("-fx-font-size:12px;-fx-text-fill:#166534;");
        double pct = total > 0 ? (double) checkedIn / total : 0;
        StackPane progressBg = new StackPane();
        progressBg.setStyle("-fx-background-color:#dcfce7;-fx-background-radius:4;-fx-pref-height:6;");
        Region progressFill = new Region();
        progressFill.setStyle("-fx-background-color:#16a34a;-fx-background-radius:4;-fx-pref-height:6;");
        progressFill.setPrefWidth(240 * pct);
        StackPane.setAlignment(progressFill, javafx.geometry.Pos.CENTER_LEFT);
        progressBg.getChildren().add(progressFill);
        Button btnClose = new Button("Đóng phiên");
        btnClose.setStyle("-fx-background-color:#dc2626;-fx-text-fill:white;" +
                          "-fx-background-radius:6;-fx-font-size:12px;-fx-cursor:hand;");
        btnClose.setOnAction(e -> {
            boxActiveSessions.getChildren().remove(card);
            lblNoSession.setVisible(true);
        });
        card.getChildren().addAll(top, lblInfo, progressBg, btnClose);
        boxActiveSessions.getChildren().add(card);
    }
}

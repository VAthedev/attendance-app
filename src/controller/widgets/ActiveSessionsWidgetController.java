package controller.widgets;

import java.util.List;
import java.util.Map;

import client.network.ServerApi;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import protocol.RequestType;

public class ActiveSessionsWidgetController {

    @FXML private VBox boxActiveSessions;
    @FXML private Label lblNoSession;

    public void loadData(String lecturerId) {
        if (lecturerId == null || lecturerId.isEmpty()) return;

        new Thread(() -> {
            try {
                protocol.Response res = ServerApi.send(RequestType.GET_ACTIVE_SESSIONS,
                        java.util.Map.of("lecturerId", lecturerId));
                if (!res.isOk()) {
                    throw new IllegalStateException(res.getMessage());
                }

                org.json.JSONArray activeSessions = ServerApi.getArray(res, "sessions");
                final List<Map<String,Object>> sessionItemsData = new java.util.ArrayList<>();

                for (int i = 0; i < activeSessions.length(); i++) {
                    org.json.JSONObject session = activeSessions.getJSONObject(i);
                    Map<String,Object> item = new java.util.HashMap<>();
                    item.put("sessionId", session.optString("sessionId", ""));
                    item.put("subject", session.optString("subject", ""));
                    item.put("status", session.optString("status", "Dang mo"));
                    item.put("openTime", session.optString("openTime", "N/A"));
                    item.put("checkedIn", session.optInt("checkedIn", 0));
                    item.put("total", session.optInt("total", 0));
                    sessionItemsData.add(item);
                }

                Platform.runLater(() -> {
                    boxActiveSessions.getChildren().clear();
                    if (sessionItemsData.isEmpty()) {
                        lblNoSession.setVisible(true);
                        lblNoSession.setManaged(true);
                    } else {
                        lblNoSession.setVisible(false);
                        lblNoSession.setManaged(false);
                        for (Map<String,Object> m : sessionItemsData) {
                            addActiveSession(
                                    (String)m.get("sessionId"),
                                    (String)m.get("subject"),
                                    (String)m.get("status"),
                                    (String)m.get("openTime"),
                                    (int)m.get("checkedIn"),
                                    (int)m.get("total"));
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void addActiveSession(String sessionId, String subject, String status, String openTime,
                                  int checkedIn, int total) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color:#f0fdf4;-fx-background-radius:8;" +
                      "-fx-border-color:#86efac;-fx-border-radius:8;-fx-border-width:1;" +
                      "-fx-padding:12 14 12 14;");
        HBox top = new HBox(8);
        Label lblSubject = new Label(subject);
        lblSubject.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#14532d;");
        HBox.setHgrow(lblSubject, Priority.ALWAYS);
        Label lblStatus = new Label("* " + status);
        lblStatus.setStyle("-fx-text-fill:#16a34a;-fx-font-size:12px;-fx-font-weight:bold;");
        top.getChildren().addAll(lblSubject, lblStatus);

        Label lblInfo = new Label("Mo luc " + openTime + "  -  " + checkedIn + "/" + total + " SV da diem danh");
        lblInfo.setStyle("-fx-font-size:12px;-fx-text-fill:#166534;");

        double pct = total > 0 ? (double) checkedIn / total : 0;
        StackPane progressBg = new StackPane();
        progressBg.setStyle("-fx-background-color:#dcfce7;-fx-background-radius:4;-fx-pref-height:6;");
        Region progressFill = new Region();
        progressFill.setStyle("-fx-background-color:#16a34a;-fx-background-radius:4;-fx-pref-height:6;");
        progressFill.setPrefWidth(240 * pct);
        StackPane.setAlignment(progressFill, javafx.geometry.Pos.CENTER_LEFT);
        progressBg.getChildren().add(progressFill);

        Button btnClose = new Button("Dong phien");
        btnClose.setStyle("-fx-background-color:#dc2626;-fx-text-fill:white;" +
                          "-fx-background-radius:6;-fx-font-size:12px;-fx-cursor:hand;");
        btnClose.setOnAction(e -> closeSession(sessionId, card));

        card.getChildren().addAll(top, lblInfo, progressBg, btnClose);
        boxActiveSessions.getChildren().add(card);
    }

    private void closeSession(String sessionId, VBox card) {
        if (sessionId == null || sessionId.isBlank()) return;

        protocol.Request req = new protocol.Request(RequestType.CLOSE_SESSION);
        req.putPayload("session_id", sessionId);
        client.network.SocketClient.getInstance().sendAsync(req, res -> {
            if (res.isOk()) {
                boxActiveSessions.getChildren().remove(card);
                boolean empty = boxActiveSessions.getChildren().isEmpty();
                lblNoSession.setVisible(empty);
                lblNoSession.setManaged(empty);
            } else {
                System.err.println("Loi dong phien: " + res.getMessage());
            }
        });
    }
}

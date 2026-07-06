package controller.widgets;

import client.network.ServerApi;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import protocol.RequestType;

public class StudentTodayScheduleWidgetController {

    @FXML private VBox boxTodaySchedule;
    @FXML private Label lblNoSchedule;

    public void loadData(String studentId) {
        if (studentId == null || studentId.isEmpty()) return;
        new Thread(() -> {
            try {
                protocol.Response res = ServerApi.send(RequestType.GET_STUDENT_TODAY_SCHEDULE,
                        java.util.Map.of("studentId", studentId));
                if (!res.isOk()) {
                    throw new IllegalStateException(res.getMessage());
                }
                org.json.JSONArray todaySchedules = ServerApi.getArray(res, "schedules");
                
                Platform.runLater(() -> {
                    boxTodaySchedule.getChildren().clear();
                    if (todaySchedules.length() == 0) {
                        lblNoSchedule.setVisible(true);
                        lblNoSchedule.setManaged(true);
                    } else {
                        lblNoSchedule.setVisible(false);
                        lblNoSchedule.setManaged(false);
                        for (int i = 0; i < todaySchedules.length(); i++) {
                            org.json.JSONObject s = todaySchedules.getJSONObject(i);
                            String subject = s.optString("subject", "Unnamed");
                            String time = s.optString("startTime", "") + " - " + s.optString("endTime", "");
                            String room = s.optString("room", "");
                            addScheduleItem(subject, time, room);
                        }
                    }
                });
            } catch(Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void addScheduleItem(String subject, String time, String room) {
        HBox item = new HBox(12);
        item.getStyleClass().add("schedule-item");
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label s = new Label(subject); s.getStyleClass().add("schedule-subject");
        Label t = new Label("⏰ " + time); t.getStyleClass().add("schedule-time");
        info.getChildren().addAll(s, t);
        Label r = new Label("P. " + room); r.getStyleClass().add("schedule-room");
        item.getChildren().addAll(info, r);
        boxTodaySchedule.getChildren().add(item);
    }
    
    @FXML
    private void handleShowAll() {
        // Navigation could be implemented via a callback or event bus
    }
}

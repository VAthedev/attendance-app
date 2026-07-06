package controller.widgets;

import java.util.List;
import java.util.Map;

import client.network.ServerApi;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import protocol.RequestType;

public class TodayScheduleWidgetController {

    @FXML private VBox boxTodaySchedule;
    @FXML private Label lblNoSchedule;

    public void loadData(String lecturerName) {
        if (lecturerName == null || lecturerName.isEmpty() || "Giang vien".equals(lecturerName)) {
            return;
        }

        new Thread(() -> {
            try {
                protocol.Response res = ServerApi.send(RequestType.GET_LECTURER_TODAY_SCHEDULE,
                        java.util.Map.of("lecturerName", lecturerName));
                if (!res.isOk()) {
                    throw new IllegalStateException(res.getMessage());
                }

                final java.util.List<java.util.Map<String,String>> scheduleItemsData = new java.util.ArrayList<>();
                org.json.JSONArray items = ServerApi.getArray(res, "items");

                for (int i = 0; i < items.length(); i++) {
                    org.json.JSONObject sch = items.getJSONObject(i);
                    Map<String,String> item = new java.util.HashMap<>();
                    item.put("subject", sch.optString("subject", ""));
                    item.put("time", sch.optString("time", "N/A"));
                    item.put("room", sch.optString("room", ""));
                    item.put("students", sch.optString("students", "0 SV"));
                    scheduleItemsData.add(item);
                }

                Platform.runLater(() -> {
                    boxTodaySchedule.getChildren().clear();
                    if (scheduleItemsData.isEmpty()) {
                        lblNoSchedule.setVisible(true); lblNoSchedule.setManaged(true);
                    } else {
                        lblNoSchedule.setVisible(false); lblNoSchedule.setManaged(false);
                        for (Map<String,String> m : scheduleItemsData) {
                            addScheduleItem(m.get("subject"), m.get("time"), m.get("room"), m.get("students"));
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void addScheduleItem(String subject, String time, String room, String students) {
        HBox item = new HBox(12);
        item.getStyleClass().add("schedule-item");
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label s = new Label(subject); s.getStyleClass().add("schedule-subject");
        Label t = new Label("⏰ " + time + "  📍 P. " + room); t.getStyleClass().add("schedule-time");
        info.getChildren().addAll(s, t);
        Label sv = new Label("👥 " + students); sv.getStyleClass().add("schedule-room");
        item.getChildren().addAll(info, sv);
        boxTodaySchedule.getChildren().add(item);
    }
}

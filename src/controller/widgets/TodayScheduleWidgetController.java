package controller.widgets;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class TodayScheduleWidgetController {

    @FXML private VBox boxTodaySchedule;
    @FXML private Label lblNoSchedule;

    public void loadData(String lecturerName) {
        if (lecturerName == null || lecturerName.isEmpty() || "Giang vien".equals(lecturerName)) {
            return;
        }

        new Thread(() -> {
            try {
                LocalDate today = LocalDate.now();
                List<Map<String, Object>> schedules = database.ScheduleRepository.getInstance().findLecturerSchedulesByDate(lecturerName, today);
                com.mongodb.client.MongoCollection<org.bson.Document> enrollmentsCol = database.DatabaseHelper.getInstance().getEnrollmentsCollection();

                final java.util.List<java.util.Map<String,String>> scheduleItemsData = new java.util.ArrayList<>();

                for (Map<String, Object> sch : schedules) {
                    String subject = (String) sch.get("subject");
                    String room = (String) sch.get("room");
                    String className = (String) sch.get("className");
                    
                    String periods = (String) sch.get("periods");
                    String time = "N/A";
                    if (periods != null) {
                        try {
                            String[] parts = periods.split(",");
                            int startP = Integer.parseInt(parts[0].trim());
                            int endP = Integer.parseInt(parts[parts.length-1].trim());
                            time = getPeriodTime(startP, true) + " - " + getPeriodTime(endP, false);
                        } catch (Exception e) {}
                    }

                    long studentCount = enrollmentsCol.countDocuments(com.mongodb.client.model.Filters.eq("subject_id", className));

                    Map<String,String> item = new java.util.HashMap<>();
                    item.put("subject", subject);
                    item.put("time", time);
                    item.put("room", room);
                    item.put("students", studentCount + " SV");
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

    private String getPeriodTime(int period, boolean isStart) {
        String[] startTimes = {"07:30", "08:20", "09:10", "10:00", "10:50", "13:00", "13:50", "14:40", "15:40", "16:30"};
        String[] endTimes = {"08:20", "09:10", "10:00", "10:50", "11:40", "13:50", "14:40", "15:30", "16:30", "17:20"};
        if (period < 1 || period > 10) return "00:00";
        return isStart ? startTimes[period - 1] : endTimes[period - 1];
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

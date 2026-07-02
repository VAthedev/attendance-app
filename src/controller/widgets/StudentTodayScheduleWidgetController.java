package controller.widgets;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class StudentTodayScheduleWidgetController {

    @FXML private VBox boxTodaySchedule;
    @FXML private Label lblNoSchedule;

    public void loadData(String studentId) {
        if (studentId == null || studentId.isEmpty()) return;
        new Thread(() -> {
            try {
                java.time.LocalDate today = java.time.LocalDate.now();
                java.util.List<java.util.Map<String,Object>> todaySchedules = database.ScheduleRepository.getInstance()
                        .findStudentSchedulesByDate(studentId, today);
                
                Platform.runLater(() -> {
                    boxTodaySchedule.getChildren().clear();
                    if (todaySchedules.isEmpty()) {
                        lblNoSchedule.setVisible(true);
                        lblNoSchedule.setManaged(true);
                    } else {
                        lblNoSchedule.setVisible(false);
                        lblNoSchedule.setManaged(false);
                        for (java.util.Map<String,Object> s : todaySchedules) {
                            String subject = (String) s.getOrDefault("subject", "Unnamed");
                            String time = s.get("startTime") + " - " + s.get("endTime");
                            String room = (String) s.getOrDefault("room", "");
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

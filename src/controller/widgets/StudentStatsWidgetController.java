package controller.widgets;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class StudentStatsWidgetController {

    @FXML private Label lblTotalSubjects;
    @FXML private Label lblAttendanceRate;
    @FXML private Label lblAbsentCount;
    @FXML private Label lblLateCount;

    public void loadData(String studentId) {
        if (studentId == null || studentId.isEmpty()) return;
        new Thread(() -> {
            try {
                java.time.LocalDate today = java.time.LocalDate.now();
                java.util.List<java.util.Map<String,Object>> allSchedules = database.ScheduleRepository.getInstance()
                        .findStudentSchedulesInRange(studentId, today.minusWeeks(2), today.plusWeeks(10));
                
                long subjectCount = allSchedules.stream().map(s -> s.get("subject")).distinct().count();
                
                Platform.runLater(() -> {
                    lblTotalSubjects.setText(String.valueOf(subjectCount));
                    lblAttendanceRate.setText("100%");
                    lblAbsentCount.setText("0");
                    lblLateCount.setText("0");
                });
            } catch(Exception e) { e.printStackTrace(); }
        }).start();
    }
}

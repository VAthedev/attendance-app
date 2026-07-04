package controller.widgets;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import service.AttendanceService;
import model.Attendance;
import java.time.LocalDate;
import java.util.List;

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
                
                AttendanceService attendanceService = new AttendanceService();
                
                // Average attendance rate
                double avgRate = attendanceService.getAverageAttendanceRate(studentId);
                
                // Monthly absent and late counts
                List<Attendance> monthlyRecords = attendanceService.getAttendanceByMonth(studentId, today.getMonthValue(), today.getYear());
                long absentCount = monthlyRecords.stream()
                        .filter(r -> "ABSENT".equals(r.getStatus()) || "UNEXCUSED_ABSENT".equals(r.getStatus()))
                        .count();
                long lateCount = monthlyRecords.stream()
                        .filter(r -> "LATE".equals(r.getStatus()))
                        .count();

                Platform.runLater(() -> {
                    lblTotalSubjects.setText(String.valueOf(subjectCount));
                    lblAttendanceRate.setText(String.format("%.1f%%", avgRate));
                    lblAbsentCount.setText(String.valueOf(absentCount));
                    lblLateCount.setText(String.valueOf(lateCount));
                });
            } catch(Exception e) { e.printStackTrace(); }
        }).start();
    }
}

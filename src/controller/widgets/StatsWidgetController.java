package controller.widgets;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class StatsWidgetController {

    @FXML private Label lblTotalSubjects;
    @FXML private Label lblTotalStudents;
    @FXML private Label lblAvgAttendance;
    @FXML private Label lblAbsentWarning;

    public void loadData(String lecturerName) {
        if (lecturerName == null || lecturerName.isEmpty() || "Giang vien".equals(lecturerName)) {
            return;
        }

        new Thread(() -> {
            try {
                LocalDate today = LocalDate.now();
                List<Map<String, Object>> schedules = database.ScheduleRepository.getInstance().findLecturerSchedulesByDate(lecturerName, today);
                com.mongodb.client.MongoCollection<org.bson.Document> enrollmentsCol = database.DatabaseHelper.getInstance().getEnrollmentsCollection();

                int totalStudentsAll = 0;
                for (Map<String, Object> sch : schedules) {
                    String className = (String) sch.get("className");
                    long studentCount = enrollmentsCol.countDocuments(com.mongodb.client.model.Filters.eq("subject_id", className));
                    totalStudentsAll += studentCount;
                }
                
                final int finalTotalStudentsAll = totalStudentsAll;

                Platform.runLater(() -> {
                    lblTotalSubjects.setText(String.valueOf(schedules.size()));
                    lblTotalStudents.setText(String.valueOf(finalTotalStudentsAll));
                    lblAvgAttendance.setText("--%");
                    lblAbsentWarning.setText("0"); // Logic cảnh báo vắng đang pending từ bản gốc
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}

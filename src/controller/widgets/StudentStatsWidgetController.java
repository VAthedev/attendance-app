package controller.widgets;

import client.network.ServerApi;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import protocol.RequestType;

public class StudentStatsWidgetController {

    @FXML private Label lblTotalSubjects;
    @FXML private Label lblAttendanceRate;
    @FXML private Label lblAbsentCount;
    @FXML private Label lblLateCount;

    public void loadData(String studentId) {
        if (studentId == null || studentId.isEmpty()) return;
        new Thread(() -> {
            try {
                protocol.Response res = ServerApi.send(RequestType.GET_STUDENT_STATS,
                        java.util.Map.of("studentId", studentId));
                if (!res.isOk()) {
                    throw new IllegalStateException(res.getMessage());
                }

                long subjectCount = Long.parseLong(res.getDataValue("totalSubjects"));
                double avgRate = Double.parseDouble(res.getDataValue("attendanceRate"));
                long absentCount = Long.parseLong(res.getDataValue("absentCount"));
                long lateCount = Long.parseLong(res.getDataValue("lateCount"));

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

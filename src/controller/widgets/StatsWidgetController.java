package controller.widgets;

import client.network.ServerApi;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import protocol.RequestType;

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
                protocol.Response res = ServerApi.send(RequestType.GET_LECTURER_DASHBOARD_STATS,
                        java.util.Map.of("lecturerName", lecturerName));
                if (!res.isOk()) {
                    throw new IllegalStateException(res.getMessage());
                }

                Platform.runLater(() -> {
                    lblTotalSubjects.setText(res.getDataValue("totalSubjects"));
                    lblTotalStudents.setText(res.getDataValue("totalStudents"));
                    lblAvgAttendance.setText(res.getDataValue("avgAttendance"));
                    lblAbsentWarning.setText(res.getDataValue("absentWarning"));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}

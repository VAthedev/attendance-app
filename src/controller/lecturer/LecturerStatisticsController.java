package controller.lecturer;

import client.network.ServerApi;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import protocol.RequestType;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class LecturerStatisticsController implements Initializable {

    @FXML private ComboBox<String> cbClasses;
    @FXML private Label lblTotalStudents;
    @FXML private Label lblTotalSessions;
    @FXML private Label lblAvgAttendance;
    @FXML private BarChart<String, Number> barChart;
    @FXML private PieChart pieChart;

    private String lecturerName;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        barChart.setAnimated(false);
        cbClasses.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadStatisticsForClass(newVal);
            }
        });
    }

    public void setLecturerName(String lecturerName) {
        this.lecturerName = lecturerName;
        loadClasses();
    }

    private void loadClasses() {
        new Thread(() -> {
            try {
                protocol.Response res = ServerApi.send(RequestType.GET_LECTURER_CLASSES,
                        java.util.Map.of("lecturerName", lecturerName));
                if (!res.isOk()) {
                    throw new IllegalStateException(res.getMessage());
                }
                List<String> classes = ServerApi.getStringList(res, "classes");
                Platform.runLater(() -> {
                    cbClasses.getItems().clear();
                    cbClasses.getItems().addAll(classes);
                    if (!classes.isEmpty()) {
                        cbClasses.getSelectionModel().selectFirst();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadStatisticsForClass(String classSelection) {
        new Thread(() -> {
            try {
                protocol.Response res = ServerApi.send(RequestType.GET_LECTURER_STATS,
                        java.util.Map.of("classSelection", classSelection));
                if (!res.isOk()) {
                    throw new IllegalStateException(res.getMessage());
                }

                long totalStudents = Long.parseLong(res.getDataValue("totalStudents"));
                int totalSessions = Integer.parseInt(res.getDataValue("totalSessions"));
                long totalPresent = Long.parseLong(res.getDataValue("totalPresent"));
                long totalAbsent = Long.parseLong(res.getDataValue("totalAbsent"));
                double avgRate = Double.parseDouble(res.getDataValue("avgRate"));

                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("So sinh vien co mat");
                org.json.JSONArray bars = ServerApi.getArray(res, "bars");
                for (int i = 0; i < bars.length(); i++) {
                    org.json.JSONObject bar = bars.getJSONObject(i);
                    series.getData().add(new XYChart.Data<>(bar.optString("label", ""), bar.optLong("presentCount", 0)));
                }

                Platform.runLater(() -> {
                    lblTotalStudents.setText(String.valueOf(totalStudents));
                    lblTotalSessions.setText(String.valueOf(totalSessions));
                    lblAvgAttendance.setText(String.format("%.1f%%", avgRate));

                    barChart.setAnimated(false);
                    barChart.getData().clear();
                    barChart.getData().add(series);

                    ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                            new PieChart.Data("Co mat (" + totalPresent + ")", totalPresent),
                            new PieChart.Data("Vang mat (" + totalAbsent + ")", totalAbsent)
                    );
                    pieChart.setData(pieData);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}

package controller.lecturer;

import database.AttendanceRepository;
import database.EnrollmentRepository;
import database.ScheduleRepository;
import database.SessionRepository;
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
import org.bson.Document;

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
                List<String> classes = ScheduleRepository.getInstance().findUniqueClassesByLecturerName(lecturerName);
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
        // classSelection is like "CTK43A - Lập trình mạng"
        String classCode = classSelection.split(" - ")[0];

        new Thread(() -> {
            try {
                // 1. Get total students
                long totalStudents = EnrollmentRepository.getInstance().countStudentsByClassCode(classCode);

                // 2. Get sessions
                List<Document> sessions = SessionRepository.getInstance().findSessionsByClassCode(classCode);
                int totalSessions = sessions.size();

                // 3. Process attendance for each session
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Số sinh viên có mặt");

                long totalPresentInAllSessions = 0;
                long totalPossibleAttendances = totalStudents * totalSessions;

                for (Document session : sessions) {
                    String sessionId = session.get("_id").toString();
                    String dateStr = session.getString("date");
                    
                    List<Document> attendances = new AttendanceRepository().findBySessionId(sessionId);
                    long presentCount = attendances.stream().filter(a -> "PRESENT".equals(a.getString("status"))).count();
                    
                    totalPresentInAllSessions += presentCount;
                    
                    series.getData().add(new XYChart.Data<>(dateStr, presentCount));
                }

                long finalTotalStudents = totalStudents;
                long finalTotalPresent = totalPresentInAllSessions;
                long finalTotalAbsent = totalPossibleAttendances - totalPresentInAllSessions;
                
                double avgRate = 0;
                if (totalPossibleAttendances > 0) {
                    avgRate = (double) totalPresentInAllSessions / totalPossibleAttendances * 100;
                }
                double finalAvgRate = avgRate;

                Platform.runLater(() -> {
                    lblTotalStudents.setText(String.valueOf(finalTotalStudents));
                    lblTotalSessions.setText(String.valueOf(totalSessions));
                    lblAvgAttendance.setText(String.format("%.1f%%", finalAvgRate));

                    barChart.getData().clear();
                    barChart.getData().add(series);

                    ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                            new PieChart.Data("Có mặt (" + finalTotalPresent + ")", finalTotalPresent),
                            new PieChart.Data("Vắng mặt (" + finalTotalAbsent + ")", finalTotalAbsent)
                    );
                    pieChart.setData(pieData);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}

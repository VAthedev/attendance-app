package controller.student;

import client.network.ServerApi;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import model.Attendance;
import protocol.RequestType;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class AttendanceStatisticController implements Initializable {

    @FXML
    private ComboBox<String> cbSemester;

    // Overview section
    @FXML
    private Label lblTotalSubjects;
    @FXML
    private Label lblAvgAttendance;
    @FXML
    private Label lblTotalClasses;
    @FXML
    private Label lblTotalPresent;
    @FXML
    private Label lblTotalAbsent;
    @FXML
    private Label lblOverallPercent;
    @FXML
    private ProgressBar overallProgress;

    // Chart section
    @FXML
    private BarChart<String, Number> barChart;
    @FXML
    private CategoryAxis xAxis;
    @FXML
    private NumberAxis yAxis;

    @FXML
    private PieChart pieChart;

    @FXML
    private LineChart<String, Number> lineChart;
    @FXML
    private CategoryAxis weekAxis;
    @FXML
    private NumberAxis percentAxis;

    // Table section
    @FXML
    private TableView<SubjectStatisticRecord> tvSubjectStats;
    @FXML
    private TableColumn<SubjectStatisticRecord, String> colSubjectName;
    @FXML
    private TableColumn<SubjectStatisticRecord, Integer> colTotalClasses;
    @FXML
    private TableColumn<SubjectStatisticRecord, Integer> colPresent;
    @FXML
    private TableColumn<SubjectStatisticRecord, Integer> colAbsent;
    @FXML
    private TableColumn<SubjectStatisticRecord, Integer> colLate;
    @FXML
    private TableColumn<SubjectStatisticRecord, String> colPercent;
    @FXML
    private TableColumn<SubjectStatisticRecord, String> colStatus;
    @FXML
    private TableColumn<SubjectStatisticRecord, String> colDetail;

    @FXML
    private VBox boxRecommendation;
    @FXML
    private Label lblRecommendation;

    private List<SubjectStatisticRecord> statisticRecords = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        barChart.setAnimated(false);
        configurePercentAxes();

        setupSemesterComboBox();
        setupTableColumns();
        loadStatistics();
    }

    private void configurePercentAxes() {
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(100);
        yAxis.setTickUnit(10);

        percentAxis.setAutoRanging(false);
        percentAxis.setLowerBound(0);
        percentAxis.setUpperBound(100);
        percentAxis.setTickUnit(10);
    }

    private void setupSemesterComboBox() {
        ObservableList<String> semesters = FXCollections.observableArrayList(
                "Học kỳ 1 (2025)", "Học kỳ 2 (2025-2026)", "Học kỳ hè (2026)");
        cbSemester.setItems(semesters);
        cbSemester.setValue("Học kỳ 2 (2025-2026)");
        cbSemester.setOnAction(e -> onSemesterChanged());
    }

    @FXML
    private void onSemesterChanged() {
        loadStatistics();
    }

    private void setupTableColumns() {
        colSubjectName.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        colTotalClasses.setCellValueFactory(new PropertyValueFactory<>("totalClasses"));
        colPresent.setCellValueFactory(new PropertyValueFactory<>("present"));
        colAbsent.setCellValueFactory(new PropertyValueFactory<>("absent"));
        colLate.setCellValueFactory(new PropertyValueFactory<>("late"));
        colPercent.setCellValueFactory(new PropertyValueFactory<>("percentage"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDetail.setCellValueFactory(new PropertyValueFactory<>("detail"));

        // Custom cell factory cho status column
        colStatus.setCellFactory(column -> new TableCell<SubjectStatisticRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("Tốt".equals(item)) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if ("Khá".equals(item)) {
                        setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                    } else if ("Cần cải thiện".equals(item)) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Custom cell factory cho percentage column
        colPercent.setCellFactory(column -> new TableCell<SubjectStatisticRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    try {
                        double percent = Double.parseDouble(item.replace("%", ""));
                        if (percent >= 80) {
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        } else if (percent >= 60) {
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        }
                    } catch (NumberFormatException e) {
                        setStyle("");
                    }
                }
            }
        });
    }

    private void loadStatistics() {
        new Thread(() -> {
            try {
                String studentId = StudentDashboardController.currentStudentId;
                if (studentId == null) {
                    throw new Exception("Không xác định được sinh viên đăng nhập.");
                }
                protocol.Response res = ServerApi.send(RequestType.GET_ATTENDANCE_STATS,
                        java.util.Map.of("studentId", studentId));
                if (!res.isOk()) {
                    throw new IllegalStateException(res.getMessage());
                }
                List<model.Attendance> rawRecords = parseAttendanceRecords(ServerApi.getArray(res, "records"));

                // Group by Subject
                Map<String, int[]> statsMap = new HashMap<>(); // [total, present, absent, late]
                for (model.Attendance att : rawRecords) {
                    String st = att.getStatus();
                    if ("EXCUSED_ABSENT".equals(st)) {
                        continue;
                    }

                    String subj = att.getSubjectName() != null ? att.getSubjectName() : "Unknown";
                    int[] counts = statsMap.getOrDefault(subj, new int[]{0, 0, 0, 0});
                    counts[0]++; // total

                    if ("PRESENT".equals(st)) counts[1]++;
                    else if ("ABSENT".equals(st) || "UNEXCUSED_ABSENT".equals(st)) counts[2]++;
                    else if ("LATE".equals(st)) counts[3]++;

                    statsMap.put(subj, counts);
                }

                List<SubjectStatisticRecord> records = new ArrayList<>();
                for (Map.Entry<String, int[]> entry : statsMap.entrySet()) {
                    String subject = entry.getKey();
                    int[] data = entry.getValue();
                    int total = data[0];
                    int present = data[1];
                    int absent = data[2];
                    int late = data[3];

                    double percentage = total > 0 ? (double) present / total * 100 : 0;
                    String status = percentage >= 80 ? "Tốt" : percentage >= 60 ? "Khá" : "Cần cải thiện";

                    records.add(new SubjectStatisticRecord(
                            subject,
                            total,
                            present,
                            absent,
                            late,
                            String.format("%.1f%%", percentage),
                            status,
                            "Chi tiết"));
                }

                Platform.runLater(() -> {
                    statisticRecords = records;
                    updateOverview();
                    updateCharts(rawRecords);
                    updateTable();
                    updateRecommendation();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Lỗi khi tải thống kê: " + e.getMessage());
                });
            }
        }).start();
    }

    private List<model.Attendance> parseAttendanceRecords(org.json.JSONArray arr) {
        List<model.Attendance> records = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            org.json.JSONObject obj = arr.getJSONObject(i);
            model.Attendance attendance = new model.Attendance();
            String date = obj.optString("date", "");
            if (!date.isBlank()) {
                try {
                    attendance.setAttendanceDate(java.time.LocalDate.parse(date));
                } catch (Exception ignored) {}
            }
            attendance.setSubjectName(obj.optString("subjectName", "Unknown"));
            attendance.setTimeString(obj.optString("timeString", ""));
            attendance.setMethod(obj.optString("method", ""));
            attendance.setStatus(obj.optString("status", ""));
            attendance.setRoom(obj.optString("room", ""));
            attendance.setLocation(obj.optString("location", ""));
            attendance.setTimestamp(obj.optLong("timestamp", 0L));
            records.add(attendance);
        }
        return records;
    }

    private void updateOverview() {
        if (statisticRecords.isEmpty()) {
            lblTotalSubjects.setText("0");
            lblAvgAttendance.setText("0%");
            lblTotalClasses.setText("0");
            lblTotalPresent.setText("0");
            lblTotalAbsent.setText("0");
            lblOverallPercent.setText("0%");
            overallProgress.setProgress(0);
            return;
        }

        int totalSubjects = statisticRecords.size();
        int totalClasses = statisticRecords.stream().mapToInt(SubjectStatisticRecord::getTotalClasses).sum();
        int totalPresent = statisticRecords.stream().mapToInt(SubjectStatisticRecord::getPresent).sum();
        int totalAbsent = statisticRecords.stream().mapToInt(SubjectStatisticRecord::getAbsent).sum();
        int totalLate = statisticRecords.stream().mapToInt(SubjectStatisticRecord::getLate).sum();

        double avgAttendance = totalClasses > 0 ? (double) totalPresent / totalClasses * 100 : 0;

        lblTotalSubjects.setText(String.valueOf(totalSubjects));
        lblAvgAttendance.setText(String.format("%.1f%%", avgAttendance));
        lblTotalClasses.setText(String.valueOf(totalClasses));
        lblTotalPresent.setText(String.valueOf(totalPresent));
        lblTotalAbsent.setText(String.valueOf(totalAbsent));
        lblOverallPercent.setText(String.format("%.1f%%", avgAttendance));
        overallProgress.setProgress(avgAttendance / 100.0);
    }

    private void updateCharts(List<model.Attendance> rawRecords) {
        updateBarChart();
        updatePieChart();
        updateLineChart(rawRecords);
    }

    private void updateBarChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tỷ lệ chuyên cần (%)");

        for (SubjectStatisticRecord record : statisticRecords) {
            double percentage = record.getTotalClasses() > 0
                    ? (double) record.getPresent() / record.getTotalClasses() * 100
                    : 0;
            series.getData().add(new XYChart.Data<>(record.getSubjectName(), percentage));
        }

        barChart.setAnimated(false);
        barChart.getData().clear();
        barChart.getData().add(series);
    }

    private void updatePieChart() {
        int totalPresent = statisticRecords.stream().mapToInt(SubjectStatisticRecord::getPresent).sum();
        int totalAbsent = statisticRecords.stream().mapToInt(SubjectStatisticRecord::getAbsent).sum();
        int totalLate = statisticRecords.stream().mapToInt(SubjectStatisticRecord::getLate).sum();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Đã điểm danh ✅", totalPresent),
                new PieChart.Data("Vắng mặt ❌", totalAbsent),
                new PieChart.Data("Đi muộn ⏰", totalLate));

        pieChart.setData(pieData);
    }

    private void updateLineChart(List<model.Attendance> rawRecords) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tỷ lệ chuyên cần theo tuần");

        Map<String, int[]> weeklyStats = new TreeMap<>(); // Map "yyyy-Www" -> [total, present]

        for (model.Attendance att : rawRecords) {
            if ("EXCUSED_ABSENT".equals(att.getStatus())) {
                continue;
            }

            java.time.LocalDate date = att.getAttendanceDate();
            long timestamp = att.getTimestamp();
            if (date == null && timestamp > 0) {
                date = java.time.Instant.ofEpochMilli(timestamp)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            }
            if (date != null) {
                int week = date.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                int year = date.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR);
                String weekKey = year + "-W" + String.format("%02d", week);

                int[] stats = weeklyStats.getOrDefault(weekKey, new int[]{0, 0});
                stats[0]++;
                if ("PRESENT".equals(att.getStatus())) {
                    stats[1]++;
                }
                weeklyStats.put(weekKey, stats);
            }
        }

        for (Map.Entry<String, int[]> entry : weeklyStats.entrySet()) {
            int total = entry.getValue()[0];
            int present = entry.getValue()[1];
            double rate = total > 0 ? (double) present / total * 100 : 0;
            series.getData().add(new XYChart.Data<>(entry.getKey(), rate));
        }

        lineChart.getData().clear();
        lineChart.getData().add(series);
    }

    private void updateTable() {
        ObservableList<SubjectStatisticRecord> data = FXCollections.observableArrayList(statisticRecords);
        tvSubjectStats.setItems(data);
    }

    private void updateRecommendation() {
        int totalRecords = statisticRecords.size();
        if (totalRecords == 0) {
            lblRecommendation.setText("Chua co du lieu diem danh de danh gia chuyen can.");
            boxRecommendation.setVisible(true);
            boxRecommendation.setManaged(true);
            return;
        }

        long poorRecords = statisticRecords.stream()
                .filter(r -> r.getTotalClasses() > 0 &&
                        (double) r.getPresent() / r.getTotalClasses() < 0.6)
                .count();

        StringBuilder recommendation = new StringBuilder();

        if (poorRecords > 0) {
            recommendation.append("⚠ Bạn có ")
                    .append(poorRecords)
                    .append(" môn có tỷ lệ chuyên cần dưới 60%. ")
                    .append("Hãy cố gắng tham gia đầy đủ các buổi học để cải thiện.");
        } else {
            long goodRecords = statisticRecords.stream()
                    .filter(r -> r.getTotalClasses() > 0 &&
                            (double) r.getPresent() / r.getTotalClasses() >= 0.8)
                    .count();

            if (goodRecords == totalRecords) {
                recommendation.append("🎉 Tuyệt vời! Bạn có tỷ lệ chuyên cầnmọi môn đều >=80%. Hãy tiếp tục duy trì!");
            } else {
                recommendation.append("👍 Chuyên cần của bạn khá tốt. Hãy tiếp tục cố gắng!");
            }
        }

        lblRecommendation.setText(recommendation.toString());
        boxRecommendation.setVisible(true);
        boxRecommendation.setManaged(true);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Inner class để hiển thị dữ liệu thống kê từng môn trong bảng
     */
    public static class SubjectStatisticRecord {
        private final String subjectName;
        private final int totalClasses;
        private final int present;
        private final int absent;
        private final int late;
        private final String percentage;
        private final String status;
        private final String detail;

        public SubjectStatisticRecord(String subjectName, int totalClasses,
                int present, int absent, int late,
                String percentage, String status, String detail) {
            this.subjectName = subjectName;
            this.totalClasses = totalClasses;
            this.present = present;
            this.absent = absent;
            this.late = late;
            this.percentage = percentage;
            this.status = status;
            this.detail = detail;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public int getTotalClasses() {
            return totalClasses;
        }

        public int getPresent() {
            return present;
        }

        public int getAbsent() {
            return absent;
        }

        public int getLate() {
            return late;
        }

        public String getPercentage() {
            return percentage;
        }

        public String getStatus() {
            return status;
        }

        public String getDetail() {
            return detail;
        }
    }
}

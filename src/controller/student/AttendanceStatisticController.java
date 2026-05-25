package controller.student;

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
import service.AttendanceService;

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

    private AttendanceService attendanceService;
    private List<SubjectStatisticRecord> statisticRecords = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        attendanceService = new AttendanceService();

        setupSemesterComboBox();
        setupTableColumns();
        loadStatistics();
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
                List<SubjectStatisticRecord> records = generateMockStatistics();

                Platform.runLater(() -> {
                    statisticRecords = records;
                    updateOverview();
                    updateCharts();
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

    private void updateOverview() {
        if (statisticRecords.isEmpty())
            return;

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

    private void updateCharts() {
        updateBarChart();
        updatePieChart();
        updateLineChart();
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

    private void updateLineChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tỷ lệ chuyên cần theo tuần");

        // Mô phỏng dữ liệu theo tuần
        String[] weeks = { "Tuần 1", "Tuần 2", "Tuần 3", "Tuần 4", "Tuần 5" };
        double[] attendanceRates = { 85.0, 87.0, 90.0, 88.0, 92.0 };

        for (int i = 0; i < weeks.length; i++) {
            series.getData().add(new XYChart.Data<>(weeks[i], attendanceRates[i]));
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
        long poorRecords = statisticRecords.stream()
                .filter(r -> r.getTotalClasses() > 0 &&
                        (double) r.getPresent() / r.getTotalClasses() < 0.6)
                .count();

        StringBuilder recommendation = new StringBuilder();

        if (poorRecords > 0) {
            recommendation.append("⚠️ Bạn có ")
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

    private List<SubjectStatisticRecord> generateMockStatistics() {
        List<SubjectStatisticRecord> records = new ArrayList<>();

        String[] subjects = { "Lập trình mạng", "Cơ sở dữ liệu", "Giải thuật", "An ninh thông tin" };
        int[][] data = {
                { 10, 9, 0, 1 }, // Lập trình mạng: 10 total, 9 present, 0 absent, 1 late
                { 10, 8, 1, 1 }, // Cơ sở dữ liệu: 10 total, 8 present, 1 absent, 1 late
                { 8, 5, 2, 1 }, // Giải thuật: 8 total, 5 present, 2 absent, 1 late
                { 10, 9, 0, 1 } // An ninh thông tin: 10 total, 9 present, 0 absent, 1 late
        };

        for (int i = 0; i < subjects.length; i++) {
            int total = data[i][0];
            int present = data[i][1];
            int absent = data[i][2];
            int late = data[i][3];

            double percentage = total > 0 ? (double) present / total * 100 : 0;
            String status = percentage >= 80 ? "Tốt" : percentage >= 60 ? "Khá" : "Cần cải thiện";

            records.add(new SubjectStatisticRecord(
                    subjects[i],
                    total,
                    present,
                    absent,
                    late,
                    String.format("%.1f%%", percentage),
                    status,
                    "Chi tiết"));
        }

        return records;
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

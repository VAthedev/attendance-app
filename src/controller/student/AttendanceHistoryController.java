package controller.student;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.Attendance;
import model.Schedule;
import model.Subject;
import service.AttendanceService;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AttendanceHistoryController implements Initializable {

    @FXML
    private ComboBox<String> cbFilterMonth;
    @FXML
    private ComboBox<String> cbFilterSubject;
    @FXML
    private Button btnFilter, btnRefresh;

    @FXML
    private Label lblTotalSessions;
    @FXML
    private Label lblPresent;
    @FXML
    private Label lblAbsent;
    @FXML
    private Label lblLate;

    @FXML
    private TableView<AttendanceRecord> tvAttendanceHistory;
    @FXML
    private TableColumn<AttendanceRecord, LocalDate> colDate;
    @FXML
    private TableColumn<AttendanceRecord, String> colSubject;
    @FXML
    private TableColumn<AttendanceRecord, String> colTime;
    @FXML
    private TableColumn<AttendanceRecord, String> colMethod;
    @FXML
    private TableColumn<AttendanceRecord, String> colStatus;
    @FXML
    private TableColumn<AttendanceRecord, String> colLocation;

    @FXML
    private ComboBox<Integer> cbPageSize;
    @FXML
    private Pagination pagination;

    @FXML
    private VBox boxLoading;
    @FXML
    private VBox boxEmpty;

    @FXML
    private Button btnExportExcel;
    @FXML
    private Button btnPrintReport;

    private AttendanceService attendanceService;
    private List<AttendanceRecord> allRecords = new ArrayList<>();
    private List<AttendanceRecord> filteredRecords = new ArrayList<>();
    private static final int ITEMS_PER_PAGE = 10;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        attendanceService = new AttendanceService();

        setupTableColumns();
        setupFilters();
        loadAttendanceData();
        setupPagination();
    }

    private void setupTableColumns() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colMethod.setCellValueFactory(new PropertyValueFactory<>("method"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));

        // Custom cell factory cho status column để hiển thị màu
        colStatus.setCellFactory(column -> new TableCell<AttendanceRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "PRESENT" -> setStyle("-fx-text-fill: #27ae60;"); // Green
                        case "ABSENT" -> setStyle("-fx-text-fill: #e74c3c;"); // Red
                        case "LATE" -> setStyle("-fx-text-fill: #f39c12;"); // Orange
                        default -> setStyle("");
                    }
                }
            }
        });
    }

    private void setupFilters() {
        // Setup month combobox
        cbFilterMonth.setItems(FXCollections.observableArrayList(
                "Tất cả", "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5",
                "Tháng 6", "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"));
        cbFilterMonth.setValue("Tất cả");

        // Setup subject combobox
        loadSubjectFilter();

        // Setup page size
        cbPageSize.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        cbPageSize.setValue(10);
        cbPageSize.setOnAction(e -> refreshTable());
    }

    private void loadSubjectFilter() {
        List<String> subjects = new ArrayList<>();
        subjects.add("Tất cả");
        subjects.addAll(allRecords.stream()
                .map(AttendanceRecord::getSubject)
                .distinct()
                .sorted()
                .collect(Collectors.toList()));

        cbFilterSubject.setItems(FXCollections.observableArrayList(subjects));
        cbFilterSubject.setValue("Tất cả");
    }

    private void loadAttendanceData() {
        showLoading(true);

        new Thread(() -> {
            try {
                // Mô phỏng load dữ liệu từ database
                List<AttendanceRecord> records = generateMockData();

                Platform.runLater(() -> {
                    allRecords = records;
                    filteredRecords = new ArrayList<>(records);
                    loadSubjectFilter();
                    updateStatistics();
                    refreshTable();
                    showLoading(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Lỗi khi tải dữ liệu: " + e.getMessage());
                    showLoading(false);
                });
            }
        }).start();
    }

    @FXML
    private void applyFilter() {
        filteredRecords = allRecords.stream()
                .filter(record -> {
                    String selectedMonth = cbFilterMonth.getValue();
                    if (!selectedMonth.equals("Tất cả")) {
                        int monthNum = Integer.parseInt(selectedMonth.split(" ")[1]);
                        if (record.getDate().getMonthValue() != monthNum) {
                            return false;
                        }
                    }

                    String selectedSubject = cbFilterSubject.getValue();
                    if (!selectedSubject.equals("Tất cả")) {
                        if (!record.getSubject().equals(selectedSubject)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        pagination.setPageCount(Math.max(1,
                (int) Math.ceil((double) filteredRecords.size() / cbPageSize.getValue())));
        pagination.setCurrentPageIndex(0);
        refreshTable();
    }

    private void updateStatistics() {
        long totalSessions = allRecords.size();
        long presentCount = allRecords.stream().filter(r -> "PRESENT".equals(r.getStatus())).count();
        long absentCount = allRecords.stream().filter(r -> "ABSENT".equals(r.getStatus())).count();
        long lateCount = allRecords.stream().filter(r -> "LATE".equals(r.getStatus())).count();

        lblTotalSessions.setText(String.valueOf(totalSessions));
        lblPresent.setText(String.valueOf(presentCount));
        lblAbsent.setText(String.valueOf(absentCount));
        lblLate.setText(String.valueOf(lateCount));
    }

    private void setupPagination() {
        pagination.setPageFactory(this::createPage);
    }

    private VBox createPage(Integer pageIndex) {
        VBox pageBox = new VBox();
        int itemsPerPage = cbPageSize.getValue();
        int fromIndex = pageIndex * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, filteredRecords.size());

        List<AttendanceRecord> pageItems = filteredRecords.subList(fromIndex, toIndex);
        ObservableList<AttendanceRecord> pageData = FXCollections.observableArrayList(pageItems);
        tvAttendanceHistory.setItems(pageData);

        boxEmpty.setVisible(pageData.isEmpty());
        boxEmpty.setManaged(pageData.isEmpty());

        return pageBox;
    }

    private void refreshTable() {
        if (pagination != null) {
            pagination.setPageCount(Math.max(1,
                    (int) Math.ceil((double) filteredRecords.size() / cbPageSize.getValue())));
            pagination.setCurrentPageIndex(0);
        }
    }

    @FXML
    private void refreshHistory() {
        cbFilterMonth.setValue("Tất cả");
        cbFilterSubject.setValue("Tất cả");
        loadAttendanceData();
    }

    @FXML
    private void exportToExcel() {
        try {
            // Placeholder for Excel export functionality
            showInfo("Chức năng xuất Excel đang được phát triển");
        } catch (Exception e) {
            showError("Lỗi khi xuất Excel: " + e.getMessage());
        }
    }

    @FXML
    private void printReport() {
        try {
            // Placeholder for print functionality
            showInfo("Chức năng in báo cáo đang được phát triển");
        } catch (Exception e) {
            showError("Lỗi khi in báo cáo: " + e.getMessage());
        }
    }

    private void showLoading(boolean show) {
        boxLoading.setVisible(show);
        boxLoading.setManaged(show);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông tin");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private List<AttendanceRecord> generateMockData() {
        List<AttendanceRecord> records = new ArrayList<>();
        String[] subjects = { "Lập trình mạng", "Cơ sở dữ liệu", "Giải thuật", "An ninh thông tin" };
        String[] methods = { "GPS", "WiFi", "QR" };
        String[] statuses = { "PRESENT", "ABSENT", "LATE" };

        LocalDate startDate = LocalDate.now().minusMonths(3);
        for (int i = 0; i < 50; i++) {
            LocalDate date = startDate.plusDays(i);
            records.add(new AttendanceRecord(
                    date,
                    subjects[i % subjects.length],
                    "07:30 - 09:10",
                    methods[i % methods.length],
                    statuses[i % 3],
                    "P." + (200 + i % 10)));
        }

        return records;
    }

    /**
     * Inner class để hiển thị dữ liệu trong bảng
     */
    public static class AttendanceRecord {
        private final LocalDate date;
        private final String subject;
        private final String time;
        private final String method;
        private final String status;
        private final String location;

        public AttendanceRecord(LocalDate date, String subject, String time,
                String method, String status, String location) {
            this.date = date;
            this.subject = subject;
            this.time = time;
            this.method = method;
            this.status = status;
            this.location = location;
        }

        public LocalDate getDate() {
            return date;
        }

        public String getSubject() {
            return subject;
        }

        public String getTime() {
            return time;
        }

        public String getMethod() {
            return method;
        }

        public String getStatus() {
            return status;
        }

        public String getLocation() {
            return location;
        }
    }
}

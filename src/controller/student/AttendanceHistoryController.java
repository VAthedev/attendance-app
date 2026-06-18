package controller.student;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.print.PrinterJob;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.time.LocalDate;
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

    private List<AttendanceRecord> allRecords = new ArrayList<>();
    private List<AttendanceRecord> filteredRecords = new ArrayList<>();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        setupFilters();
        loadAttendanceData();
    }

    private void setupTableColumns() {
        // Cột Date với format
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDate.setCellFactory(column -> new TableCell<AttendanceRecord, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(dateFormatter));
                }
            }
        });

        colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colMethod.setCellValueFactory(new PropertyValueFactory<>("method"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));

        // Custom status column
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<AttendanceRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    switch (item) {
                        case "PRESENT":
                            setText("✅ Có mặt");
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            break;
                        case "ABSENT":
                            setText("❌ Vắng mặt");
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            break;
                        case "LATE":
                            setText("⏰ Đi muộn");
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                            break;
                        default:
                            setText(item);
                            setStyle("");
                    }
                }
            }
        });
    }

    private void setupFilters() {
        cbFilterMonth.setItems(FXCollections.observableArrayList(
                "Tất cả", "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5",
                "Tháng 6", "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"));
        cbFilterMonth.setValue("Tất cả");

        cbPageSize.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        cbPageSize.setValue(10);
        cbPageSize.setOnAction(e -> {
            refreshTable();
            setupPagination();
        });
    }

    private void loadSubjectFilter() {
        Set<String> subjects = new TreeSet<>();
        subjects.add("Tất cả");
        subjects.addAll(allRecords.stream()
                .map(AttendanceRecord::getSubject)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        cbFilterSubject.setItems(FXCollections.observableArrayList(subjects));
        cbFilterSubject.setValue("Tất cả");
    }

    private void loadAttendanceData() {
        showLoading(true);

        new Thread(() -> {
            try {
                List<AttendanceRecord> records = generateMockData();

                Platform.runLater(() -> {
                    allRecords = records;
                    filteredRecords = new ArrayList<>(records);
                    loadSubjectFilter();
                    updateStatistics();
                    setupPagination();
                    showLoading(false);

                    boxEmpty.setVisible(records.isEmpty());
                    boxEmpty.setManaged(records.isEmpty());
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
        String selectedMonth = cbFilterMonth.getValue();
        String selectedSubject = cbFilterSubject.getValue();

        filteredRecords = allRecords.stream()
                .filter(record -> {
                    if (selectedMonth != null && !selectedMonth.equals("Tất cả")) {
                        int monthNum = Integer.parseInt(selectedMonth.split(" ")[1]);
                        if (record.getDate().getMonthValue() != monthNum) {
                            return false;
                        }
                    }

                    if (selectedSubject != null && !selectedSubject.equals("Tất cả")) {
                        if (!record.getSubject().equals(selectedSubject)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        updateStatistics();
        refreshTable();
        setupPagination();

        boxEmpty.setVisible(filteredRecords.isEmpty());
        boxEmpty.setManaged(filteredRecords.isEmpty());
    }

    private void updateStatistics() {
        long totalSessions = filteredRecords.size();
        long presentCount = filteredRecords.stream()
                .filter(r -> "PRESENT".equals(r.getStatus())).count();
        long absentCount = filteredRecords.stream()
                .filter(r -> "ABSENT".equals(r.getStatus())).count();
        long lateCount = filteredRecords.stream()
                .filter(r -> "LATE".equals(r.getStatus())).count();

        lblTotalSessions.setText(String.valueOf(totalSessions));
        lblPresent.setText(String.valueOf(presentCount));
        lblAbsent.setText(String.valueOf(absentCount));
        lblLate.setText(String.valueOf(lateCount));
    }

    private void setupPagination() {
        int pageSize = cbPageSize.getValue();
        int pageCount = (int) Math.ceil((double) filteredRecords.size() / pageSize);
        pagination.setPageCount(Math.max(1, pageCount));
        pagination.setCurrentPageIndex(0);
        pagination.setPageFactory(this::createPage);
    }

    private VBox createPage(Integer pageIndex) {
        int pageSize = cbPageSize.getValue();
        int fromIndex = pageIndex * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filteredRecords.size());

        if (fromIndex >= filteredRecords.size()) {
            tvAttendanceHistory.setItems(FXCollections.observableArrayList());
        } else {
            List<AttendanceRecord> pageItems = filteredRecords.subList(fromIndex, toIndex);
            ObservableList<AttendanceRecord> pageData = FXCollections.observableArrayList(pageItems);
            tvAttendanceHistory.setItems(pageData);
        }

        return new VBox();
    }

    private void refreshTable() {
        if (pagination != null) {
            int pageSize = cbPageSize.getValue();
            int pageCount = (int) Math.ceil((double) filteredRecords.size() / pageSize);
            pagination.setPageCount(Math.max(1, pageCount));
        }
    }

    @FXML
    private void refreshHistory() {
        cbFilterMonth.setValue("Tất cả");
        cbFilterSubject.setValue("Tất cả");
        filteredRecords = new ArrayList<>(allRecords);
        updateStatistics();
        refreshTable();
        setupPagination();
        boxEmpty.setVisible(filteredRecords.isEmpty());
    }

    @FXML
    private void exportToExcel() {
        if (filteredRecords == null || filteredRecords.isEmpty()) {
            showInfo("Không có dữ liệu để xuất");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu báo cáo điểm danh");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fileChooser.setInitialFileName("LichSuDiemDanh_" + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".xlsx");
        
        File file = fileChooser.showSaveDialog(tvAttendanceHistory.getScene().getWindow());
        if (file == null) {
            return;
        }

        new Thread(() -> {
            Platform.runLater(() -> showLoading(true));
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Lịch sử điểm danh");

                // Header
                Row headerRow = sheet.createRow(0);
                String[] columns = {"Ngày", "Môn học", "Thời gian", "Phương thức", "Trạng thái", "Vị trí"};
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                }

                // Data
                int rowNum = 1;
                for (AttendanceRecord record : filteredRecords) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(record.getDate() != null ? record.getDate().format(dateFormatter) : "");
                    row.createCell(1).setCellValue(record.getSubject() != null ? record.getSubject() : "");
                    row.createCell(2).setCellValue(record.getTime() != null ? record.getTime() : "");
                    row.createCell(3).setCellValue(record.getMethod() != null ? record.getMethod() : "");
                    String status = record.getStatus() != null ? record.getStatus() : "";
                    String statusText = switch (status) {
                        case "PRESENT" -> "Có mặt";
                        case "ABSENT" -> "Vắng mặt";
                        case "LATE" -> "Đi muộn";
                        default -> status;
                    };
                    row.createCell(4).setCellValue(statusText);
                    row.createCell(5).setCellValue(record.getLocation() != null ? record.getLocation() : "");
                }

                // Resize columns
                for (int i = 0; i < columns.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                try (FileOutputStream fileOut = new FileOutputStream(file)) {
                    workbook.write(fileOut);
                }

                Platform.runLater(() -> {
                    showLoading(false);
                    showInfo("Đã xuất Excel thành công tại:\n" + file.getAbsolutePath());
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showLoading(false);
                    showError("Lỗi khi xuất Excel: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void printReport() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(tvAttendanceHistory.getScene().getWindow())) {
            boolean success = job.printPage(tvAttendanceHistory);
            if (success) {
                job.endJob();
                showInfo("In báo cáo thành công");
            } else {
                showError("Không thể in báo cáo");
            }
        }
    }

    private void showLoading(boolean show) {
        boxLoading.setVisible(show);
        boxLoading.setManaged(show);
        tvAttendanceHistory.setVisible(!show);
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
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private List<AttendanceRecord> generateMockData() {
        List<AttendanceRecord> records = new ArrayList<>();
        String[] subjects = { "Lập trình mạng", "Cơ sở dữ liệu", "Giải thuật", "An ninh thông tin" };
        String[] methods = { "GPS", "WiFi", "QR Code" };
        String[] statuses = { "PRESENT", "ABSENT", "LATE" };
        String[] rooms = { "P.201", "P.305", "P.108", "P.412" };

        LocalDate startDate = LocalDate.now().minusMonths(3);

        for (int i = 0; i < 30; i++) {
            LocalDate date = startDate.plusDays(i * 2);
            records.add(new AttendanceRecord(
                    date,
                    subjects[i % subjects.length],
                    "07:30 - 09:10",
                    methods[i % methods.length],
                    statuses[i % 3],
                    rooms[i % rooms.length]));
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
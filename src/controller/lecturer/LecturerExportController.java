package controller.lecturer;

import database.AttendanceRepository;
import database.EnrollmentRepository;
import database.ScheduleRepository;
import database.SessionRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import org.bson.Document;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class LecturerExportController implements Initializable {

    @FXML private ComboBox<String> cbClasses;
    @FXML private TableView<StudentExportData> tableStudents;
    @FXML private TableColumn<StudentExportData, String> colStudentId;
    @FXML private TableColumn<StudentExportData, String> colStudentName;
    @FXML private TableColumn<StudentExportData, String> colPresent;
    @FXML private TableColumn<StudentExportData, String> colAbsent;
    @FXML private TableColumn<StudentExportData, String> colAttendanceRate;

    private String lecturerName;
    private ObservableList<StudentExportData> studentDataList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        cbClasses.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadTableDataForClass(newVal);
            }
        });
    }

    private void setupTable() {
        colStudentId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().studentId));
        colStudentName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().studentName));
        colPresent.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().presentCount)));
        colAbsent.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().absentCount)));
        colAttendanceRate.setCellValueFactory(cellData -> {
            double rate = cellData.getValue().getAttendanceRate();
            return new SimpleStringProperty(String.format("%.1f%%", rate));
        });
        tableStudents.setItems(studentDataList);
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

    private void loadTableDataForClass(String classSelection) {
        String classCode = classSelection.split(" - ")[0];

        new Thread(() -> {
            try {
                List<Document> enrollments = EnrollmentRepository.getInstance().findStudentsByClassCode(classCode);
                List<Document> sessions = SessionRepository.getInstance().findSessionsByClassCode(classCode);

                Map<String, StudentExportData> studentMap = new HashMap<>();

                for (Document enrollment : enrollments) {
                    String studentId = enrollment.getString("student_id");
                    Document studentDetails = enrollment.get("student_details", Document.class);
                    String studentName = studentDetails != null ? studentDetails.getString("full_name") : "Không rõ";
                    
                    studentMap.put(studentId, new StudentExportData(studentId, studentName, sessions.size()));
                }

                for (Document session : sessions) {
                    String sessionId = session.getObjectId("_id").toHexString();
                    List<Document> attendances = new AttendanceRepository().findBySessionId(sessionId);

                    for (Document attendance : attendances) {
                        String studentId = attendance.getString("student_id");
                        String status = attendance.getString("status");
                        
                        StudentExportData data = studentMap.get(studentId);
                        if (data != null) {
                            if ("PRESENT".equals(status)) {
                                data.presentCount++;
                            } else {
                                data.absentCount++;
                            }
                        }
                    }
                }

                Platform.runLater(() -> {
                    studentDataList.clear();
                    studentDataList.addAll(studentMap.values());
                    
                    // Sort by student ID
                    studentDataList.sort((d1, d2) -> d1.studentId.compareTo(d2.studentId));
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleExportCSV() {
        if (studentDataList.isEmpty()) {
            showAlert("Lỗi", "Không có dữ liệu để xuất.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu file danh sách điểm danh");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        
        String classSelection = cbClasses.getValue();
        String defaultFileName = "Danh_sach_diem_danh";
        if (classSelection != null) {
            defaultFileName += "_" + classSelection.split(" - ")[0];
        }
        fileChooser.setInitialFileName(defaultFileName + ".csv");

        File file = fileChooser.showSaveDialog(cbClasses.getScene().getWindow());
        if (file != null) {
            saveDataToFile(file);
        }
    }

    private void saveDataToFile(File file) {
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            // Write BOM for UTF-8 Excel compatibility
            writer.write('\ufeff');
            
            writer.println("MSSV,Họ và Tên,Số buổi có mặt,Số buổi vắng,Tỉ lệ chuyên cần (%)");
            
            for (StudentExportData data : studentDataList) {
                String line = String.format("%s,%s,%d,%d,%.1f",
                        escapeCSV(data.studentId),
                        escapeCSV(data.studentName),
                        data.presentCount,
                        data.absentCount,
                        data.getAttendanceRate());
                writer.println(line);
            }
            
            showAlert("Thành công", "Đã lưu danh sách điểm danh thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể lưu file: " + e.getMessage());
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(title.equals("Lỗi") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class StudentExportData {
        public String studentId;
        public String studentName;
        public int totalSessions;
        public int presentCount = 0;
        public int absentCount = 0;

        public StudentExportData(String studentId, String studentName, int totalSessions) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.totalSessions = totalSessions;
        }

        public double getAttendanceRate() {
            if (totalSessions == 0) return 0;
            return (double) presentCount / totalSessions * 100;
        }
    }
}

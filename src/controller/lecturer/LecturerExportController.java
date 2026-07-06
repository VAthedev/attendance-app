package controller.lecturer;

import client.network.ServerApi;
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
import protocol.RequestType;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
        colAttendanceRate.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.1f%%", cellData.getValue().getAttendanceRate())));
        tableStudents.setItems(studentDataList);
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

    private void loadTableDataForClass(String classSelection) {
        new Thread(() -> {
            try {
                protocol.Response res = ServerApi.send(RequestType.GET_LECTURER_EXPORT_DATA,
                        java.util.Map.of("classSelection", classSelection));
                if (!res.isOk()) {
                    throw new IllegalStateException(res.getMessage());
                }

                org.json.JSONArray students = ServerApi.getArray(res, "students");
                java.util.List<StudentExportData> rows = new java.util.ArrayList<>();
                for (int i = 0; i < students.length(); i++) {
                    org.json.JSONObject student = students.getJSONObject(i);
                    StudentExportData data = new StudentExportData(
                            student.optString("studentId", ""),
                            student.optString("studentName", ""),
                            student.optInt("totalSessions", 0));
                    data.presentCount = student.optInt("presentCount", 0);
                    data.absentCount = student.optInt("absentCount", 0);
                    rows.add(data);
                }

                Platform.runLater(() -> {
                    studentDataList.clear();
                    studentDataList.addAll(rows);
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
            showAlert("Loi", "Khong co du lieu de xuat.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Luu file danh sach diem danh");
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
            writer.write('\ufeff');
            writer.println("MSSV,Ho va Ten,So buoi co mat,So buoi vang,Ti le chuyen can (%)");

            for (StudentExportData data : studentDataList) {
                String line = String.format("%s,%s,%d,%d,%.1f",
                        escapeCSV(data.studentId),
                        escapeCSV(data.studentName),
                        data.presentCount,
                        data.absentCount,
                        data.getAttendanceRate());
                writer.println(line);
            }

            showAlert("Thanh cong", "Da luu danh sach diem danh thanh cong!");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Loi", "Khong the luu file: " + e.getMessage());
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
        Alert alert = new Alert(title.equals("Loi") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
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

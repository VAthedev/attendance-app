package controller.lecturer;

import database.AttendanceRepository;
import database.EnrollmentRepository;
import database.SessionRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LecturerAttendanceListController {

    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private TextField txtSearch;
    @FXML private VBox listContainer;

    private String lecturerId;
    private List<SessionData> allSessions = new ArrayList<>();

    @FXML
    public void initialize() {
        cmbStatusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Đang mở", "Đã đóng"));
        cmbStatusFilter.setValue("Tất cả");
        cmbStatusFilter.setOnAction(e -> filterAndRender());
        
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterAndRender());
    }

    public void setLecturerId(String lecturerId) {
        this.lecturerId = lecturerId;
        loadData();
    }

    private void loadData() {
        listContainer.getChildren().clear();
        Label loadingLabel = new Label("Đang tải dữ liệu...");
        loadingLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
        listContainer.getChildren().add(loadingLabel);

        new Thread(() -> {
            try {
                SessionRepository sessionRepo = SessionRepository.getInstance();
                AttendanceRepository attRepo = new AttendanceRepository();
                EnrollmentRepository enrRepo = EnrollmentRepository.getInstance();

                List<Document> sessions = sessionRepo.findAllSessionsByLecturerId(lecturerId);
                List<SessionData> dataList = new ArrayList<>();

                for (Document doc : sessions) {
                    SessionData data = new SessionData();
                    data.id = doc.get("_id").toString();
                    data.className = doc.getString("class_code");
                    data.subject = doc.getString("subject_code");
                    data.date = doc.getString("date");
                    data.startTime = doc.getString("start_time");
                    data.endTime = doc.getString("end_time");
                    data.status = doc.getString("status");

                    data.presentCount = attRepo.countBySessionId(data.id);
                    data.totalStudents = enrRepo.countStudentsByClassCode(data.className);
                    
                    dataList.add(data);
                }

                Platform.runLater(() -> {
                    this.allSessions = dataList;
                    filterAndRender();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    listContainer.getChildren().clear();
                    listContainer.getChildren().add(new Label("Lỗi khi tải dữ liệu: " + e.getMessage()));
                });
            }
        }).start();
    }

    @FXML
    private void handleSearch() {
        filterAndRender();
    }

    private void filterAndRender() {
        listContainer.getChildren().clear();
        
        String filterStatus = cmbStatusFilter.getValue();
        String keyword = txtSearch.getText().toLowerCase();

        List<SessionData> filtered = allSessions.stream().filter(s -> {
            boolean matchStatus = "Tất cả".equals(filterStatus) ||
                                  ("Đang mở".equals(filterStatus) && "OPEN".equals(s.status)) ||
                                  ("Đã đóng".equals(filterStatus) && "CLOSED".equals(s.status));
            boolean matchKeyword = keyword.isEmpty() || 
                                   (s.className != null && s.className.toLowerCase().contains(keyword)) ||
                                   (s.subject != null && s.subject.toLowerCase().contains(keyword));
            return matchStatus && matchKeyword;
        }).collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Label noDataLabel = new Label("Không tìm thấy phiên điểm danh nào.");
            noDataLabel.setStyle("-fx-text-fill: #999; -fx-padding: 20;");
            listContainer.getChildren().add(noDataLabel);
            return;
        }

        for (SessionData s : filtered) {
            listContainer.getChildren().add(createCard(s));
        }
    }

    private HBox createCard(SessionData s) {
        HBox card = new HBox(15);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 15; " +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setAlignment(Pos.CENTER_LEFT);

        // Icon/Status indicator
        VBox statusBox = new VBox();
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPrefWidth(60);
        Label icon = new Label("OPEN".equals(s.status) ? "🟢" : "⚫");
        icon.setStyle("-fx-font-size: 24px;");
        statusBox.getChildren().add(icon);

        // Info
        VBox infoBox = new VBox(5);
        Label lblClass = new Label(s.className + " - " + s.subject);
        lblClass.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");
        Label lblTime = new Label("Ngày: " + s.date + " | Từ " + s.startTime + " đến " + s.endTime);
        lblTime.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        infoBox.getChildren().addAll(lblClass, lblTime);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Progress
        VBox progressBox = new VBox(5);
        progressBox.setAlignment(Pos.CENTER_RIGHT);
        progressBox.setPrefWidth(200);
        
        long total = Math.max(1, s.totalStudents); // Avoid div by zero
        double progress = (double) s.presentCount / total;
        
        Label lblStats = new Label(s.presentCount + " / " + s.totalStudents + " Sinh viên");
        lblStats.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a237e;");
        
        ProgressBar pBar = new ProgressBar(progress);
        pBar.setPrefWidth(180);
        pBar.setStyle("-fx-accent: " + (progress > 0.8 ? "#4caf50" : (progress > 0.5 ? "#ff9800" : "#f44336")) + ";");
        
        progressBox.getChildren().addAll(lblStats, pBar);

        // Status Badge
        Label lblStatus = new Label("OPEN".equals(s.status) ? "Đang mở" : "Đã đóng");
        lblStatus.setStyle("-fx-padding: 5 10; -fx-background-radius: 15; -fx-font-size: 11px; -fx-font-weight: bold; " +
                           ("OPEN".equals(s.status) ? "-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32;" 
                                                    : "-fx-background-color: #f5f5f5; -fx-text-fill: #757575;"));

        card.getChildren().addAll(statusBox, infoBox, progressBox, lblStatus);
        return card;
    }

    private static class SessionData {
        String id;
        String className;
        String subject;
        String date;
        String startTime;
        String endTime;
        String status;
        long presentCount;
        long totalStudents;
    }
}

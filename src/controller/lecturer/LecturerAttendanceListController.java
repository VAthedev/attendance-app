package controller.lecturer;

import client.network.ServerApi;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import protocol.RequestType;

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
        cmbStatusFilter.setItems(FXCollections.observableArrayList("Tat ca", "Dang mo", "Da dong"));
        cmbStatusFilter.setValue("Tat ca");
        cmbStatusFilter.setOnAction(e -> filterAndRender());

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterAndRender());
    }

    public void setLecturerId(String lecturerId) {
        this.lecturerId = lecturerId;
        loadData();
    }

    private void loadData() {
        listContainer.getChildren().clear();
        Label loadingLabel = new Label("Dang tai du lieu...");
        loadingLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
        listContainer.getChildren().add(loadingLabel);

        new Thread(() -> {
            try {
                protocol.Response res = ServerApi.send(RequestType.GET_LECTURER_ATTENDANCE_LIST,
                        java.util.Map.of("lecturerId", lecturerId));
                if (!res.isOk()) {
                    throw new IllegalStateException(res.getMessage());
                }

                org.json.JSONArray sessions = ServerApi.getArray(res, "sessions");
                List<SessionData> dataList = new ArrayList<>();
                for (int i = 0; i < sessions.length(); i++) {
                    org.json.JSONObject obj = sessions.getJSONObject(i);
                    SessionData data = new SessionData();
                    data.id = obj.optString("id", "");
                    data.className = obj.optString("className", "");
                    data.subject = obj.optString("subject", "");
                    data.date = obj.optString("date", "N/A");
                    data.startTime = obj.optString("startTime", "N/A");
                    data.endTime = obj.optString("endTime", "N/A");
                    data.status = obj.optString("status", "");
                    data.presentCount = obj.optLong("presentCount", 0);
                    data.totalStudents = obj.optLong("totalStudents", 0);
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
                    listContainer.getChildren().add(new Label("Loi khi tai du lieu: " + e.getMessage()));
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
        String keyword = txtSearch.getText() != null ? txtSearch.getText().toLowerCase() : "";

        List<SessionData> filtered = allSessions.stream().filter(s -> {
            boolean matchStatus = "Tat ca".equals(filterStatus) ||
                    ("Dang mo".equals(filterStatus) && "OPEN".equals(s.status)) ||
                    ("Da dong".equals(filterStatus) && "CLOSED".equals(s.status));
            boolean matchKeyword = keyword.isEmpty() ||
                    (s.className != null && s.className.toLowerCase().contains(keyword)) ||
                    (s.subject != null && s.subject.toLowerCase().contains(keyword));
            return matchStatus && matchKeyword;
        }).collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Label noDataLabel = new Label("Khong tim thay phien diem danh nao.");
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

        VBox statusBox = new VBox();
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPrefWidth(60);
        Label icon = new Label("OPEN".equals(s.status) ? "OPEN" : "DONE");
        icon.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        statusBox.getChildren().add(icon);

        VBox infoBox = new VBox(5);
        Label lblClass = new Label(s.className + " - " + s.subject);
        lblClass.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");
        Label lblTime = new Label("Ngay: " + s.date + " | Tu " + s.startTime + " den " + s.endTime);
        lblTime.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        infoBox.getChildren().addAll(lblClass, lblTime);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        VBox progressBox = new VBox(5);
        progressBox.setAlignment(Pos.CENTER_RIGHT);
        progressBox.setPrefWidth(200);

        long total = Math.max(1, s.totalStudents);
        double progress = (double) s.presentCount / total;

        Label lblStats = new Label(s.presentCount + " / " + s.totalStudents + " Sinh vien");
        lblStats.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a237e;");

        ProgressBar pBar = new ProgressBar(progress);
        pBar.setPrefWidth(180);
        pBar.setStyle("-fx-accent: " + (progress > 0.8 ? "#4caf50" : (progress > 0.5 ? "#ff9800" : "#f44336")) + ";");

        progressBox.getChildren().addAll(lblStats, pBar);

        Label lblStatus = new Label("OPEN".equals(s.status) ? "Dang mo" : "Da dong");
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

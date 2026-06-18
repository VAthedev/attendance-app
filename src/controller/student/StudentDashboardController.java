package controller.student;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import util.FxmlUtil;

public class StudentDashboardController implements Initializable {

    @FXML private Button btnDashboard, btnScheduleDay, btnScheduleWeek;
    @FXML private Button btnScheduleSubject, btnAttendance, btnHistory;
    @FXML private Button btnStats, btnChat, btnNotification;

    @FXML private Label lblPageTitle, lblPageDate;
    @FXML private Label lblUserName, lblUserId;
    @FXML private Label lblTotalSubjects, lblAttendanceRate;
    @FXML private Label lblAbsentCount, lblLateCount;

    @FXML private VBox  boxTodaySchedule, boxRecentAttendance, boxNotifications;
    @FXML private Label lblNoSchedule, lblNoAttendance, lblNoNotif;

    @FXML private ScrollPane paneDashboard;
    @FXML private Label      paneComingSoon;

    // Content area de nhung man hinh con
    @FXML private StackPane contentArea;

    private Node currentSubPane = null;
    public static String currentStudentId = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        String today = LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy",
            new java.util.Locale("vi", "VN")));
        lblPageDate.setText(today);
    }

    public void setUserInfo(String fullName, String studentId, String token) {
        lblUserName.setText(fullName.isEmpty() ? "Sinh vien" : fullName);
        lblUserId.setText("MSSV: " + studentId);
        currentStudentId = studentId;
        // Đăng ký PushListener để nhận thông báo thời gian thực
        client.network.SocketClient.getInstance().addPushListener(res -> {
            if ("ANNOUNCEMENT".equals(res.getMessage())) {
                String message = res.getDataValue("message");
                if ("SCHEDULE_UPDATED".equals(message)) {
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Thông báo");
                        alert.setHeaderText("Đồng bộ Thời khóa biểu");
                        alert.setContentText("Thời khóa biểu đã có sự thay đổi. Vui lòng làm mới trang.");
                        alert.show();
                        loadDashboardData(studentId);
                    });
                } else if (message != null && message.startsWith("SESSION_CLOSED:")) {
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                        alert.setTitle("Thông báo Điểm danh");
                        alert.setHeaderText("Phiên điểm danh đã đóng");
                        alert.setContentText("Hệ thống đã tự động đóng phiên điểm danh (hết giờ).");
                        alert.show();
                        showHome(); // Đẩy user về màn hình chính
                    });
                }
            }
        });

        loadDashboardData(studentId);
    }

    private void loadDashboardData(String studentId) {
        new Thread(() -> {
            try {
                java.time.LocalDate today = java.time.LocalDate.now();
                java.util.List<java.util.Map<String,Object>> allSchedules = database.ScheduleRepository.getInstance()
                        .findStudentSchedulesInRange(studentId, today.minusWeeks(2), today.plusWeeks(10));
                
                long subjectCount = allSchedules.stream().map(s -> s.get("subject")).distinct().count();
                
                java.util.List<java.util.Map<String,Object>> todaySchedules = database.ScheduleRepository.getInstance()
                        .findStudentSchedulesByDate(studentId, today);
                
                javafx.application.Platform.runLater(() -> {
                    lblTotalSubjects.setText(String.valueOf(subjectCount));
                    
                    lblAttendanceRate.setText("100%");
                    lblAbsentCount.setText("0");
                    lblLateCount.setText("0");
                    
                    boxTodaySchedule.getChildren().clear();
                    if (todaySchedules.isEmpty()) {
                        lblNoSchedule.setVisible(true);
                        lblNoSchedule.setManaged(true);
                    } else {
                        lblNoSchedule.setVisible(false);
                        lblNoSchedule.setManaged(false);
                        for (java.util.Map<String,Object> s : todaySchedules) {
                            String subject = (String) s.getOrDefault("subject", "Unnamed");
                            String time = s.get("startTime") + " - " + s.get("endTime");
                            String room = (String) s.getOrDefault("room", "");
                            addScheduleItem(subject, time, room);
                        }
                    }
                    
                    boxRecentAttendance.getChildren().clear();
                    boxNotifications.getChildren().clear();
                    
                    if (lblNoAttendance != null) {
                        lblNoAttendance.setVisible(true);
                        lblNoAttendance.setManaged(true);
                    }
                    if (lblNoNotif != null) {
                        lblNoNotif.setVisible(true);
                        lblNoNotif.setManaged(true);
                    }
                });
            } catch(Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void addScheduleItem(String subject, String time, String room) {
        HBox item = new HBox(12);
        item.getStyleClass().add("schedule-item");
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label s = new Label(subject); s.getStyleClass().add("schedule-subject");
        Label t = new Label("⏰ " + time); t.getStyleClass().add("schedule-time");
        info.getChildren().addAll(s, t);
        Label r = new Label("🏫 " + room); r.getStyleClass().add("schedule-room");
        item.getChildren().addAll(info, r);
        boxTodaySchedule.getChildren().add(item);
        lblNoSchedule.setVisible(false); lblNoSchedule.setManaged(false);
    }

    private void addAttendanceItem(String subject, String date, String status) {
        HBox item = new HBox(12);
        item.setStyle("-fx-padding:8 0 8 0;");
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label s = new Label(subject); s.getStyleClass().add("schedule-subject");
        Label d = new Label(date);    d.getStyleClass().add("schedule-time");
        info.getChildren().addAll(s, d);
        String cls  = switch(status) { case "PRESENT"->"badge-present"; case "ABSENT"->"badge-absent"; default->"badge-late"; };
        String text = switch(status) { case "PRESENT"->"Có mặt"; case "ABSENT"->"Vắng"; default->"Trễ"; };
        Label badge = new Label(text); badge.getStyleClass().add(cls);
        item.getChildren().addAll(info, badge);
        boxRecentAttendance.getChildren().add(item);
    }

    private void addNotificationItem(String title, String message) {
        VBox item = new VBox(4);
        item.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:8;" +
                      "-fx-padding:10 14 10 14;-fx-border-color:#e2e8f0;" +
                      "-fx-border-radius:8;-fx-border-width:1;");
        Label t = new Label(title);   t.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1a2744;");
        Label m = new Label(message); m.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7a99;");
        item.getChildren().addAll(t, m);
        boxNotifications.getChildren().add(item);
        lblNoNotif.setVisible(false); lblNoNotif.setManaged(false);
    }

    // ===== NAVIGATION =====

    @FXML private void showDashboard() {
        setActiveBtn(btnDashboard); lblPageTitle.setText("Tổng quan");
        removeSubPane();
        paneDashboard.setVisible(true); paneDashboard.setManaged(true);
        paneComingSoon.setVisible(false); paneComingSoon.setManaged(false);
    }

    @FXML private void showAttendance() {
        setActiveBtn(btnAttendance); lblPageTitle.setText("Điểm danh");
        loadSubPane("/fxml/student/AttendanceGPS.fxml");
    }

    @FXML private void showScheduleDay()     { setActiveBtn(btnScheduleDay);     lblPageTitle.setText("TKB theo ngày");       loadSubPane("/fxml/student/ScheduleDay.fxml"); }
    @FXML private void showScheduleWeek()    { setActiveBtn(btnScheduleWeek);    lblPageTitle.setText("TKB theo tuần");       loadSubPane("/fxml/student/ScheduleWeek.fxml"); }
    @FXML private void showScheduleSubject() { setActiveBtn(btnScheduleSubject); lblPageTitle.setText("TKB theo môn");        loadSubPane("/fxml/student/ScheduleSubject.fxml"); }
    @FXML private void showHistory()         { setActiveBtn(btnHistory);         lblPageTitle.setText("Lịch sử điểm danh");  loadSubPane("/fxml/student/AttendanceHistory.fxml"); }
    @FXML private void showStats()           { setActiveBtn(btnStats);           lblPageTitle.setText("Thống kê chuyên cần"); loadSubPane("/fxml/student/AttendanceStats.fxml"); }
    @FXML private void showChat()            { setActiveBtn(btnChat);            lblPageTitle.setText("Chat nội bộ lớp học"); loadSubPane("/fxml/shared/Chat.fxml"); }
    @FXML private void showNotification()    { setActiveBtn(btnNotification);    lblPageTitle.setText("Thông báo");           showComing(); }

    @FXML private void handleLogout() {
        loadScene("/fxml/auth/Login.fxml", "Dang nhap");
    }

    private void showComing() {
        removeSubPane();
        paneDashboard.setVisible(false);  paneDashboard.setManaged(false);
        paneComingSoon.setVisible(true);  paneComingSoon.setManaged(true);
    }

    private void loadSubPane(String fxmlPath) {
        try {
            removeSubPane();
            paneDashboard.setVisible(false);  paneDashboard.setManaged(false);
            paneComingSoon.setVisible(false);  paneComingSoon.setManaged(false);

            FXMLLoader loader = FxmlUtil.loader(fxmlPath);
            Node node = loader.load();
            contentArea.getChildren().add(node);
            currentSubPane = node;
        } catch (Exception e) {
            System.err.println("Loi load pane: " + e.getMessage());
            showComing();
        }
    }

    private void removeSubPane() {
        if (currentSubPane != null) {
            contentArea.getChildren().remove(currentSubPane);
            currentSubPane = null;
        }
    }

    private void setActiveBtn(Button active) {
        Button[] all = {btnDashboard, btnScheduleDay, btnScheduleWeek,
                        btnScheduleSubject, btnAttendance, btnHistory,
                        btnStats, btnChat, btnNotification};
        for (Button b : all) b.getStyleClass().remove("sidebar-btn-active");
        active.getStyleClass().add("sidebar-btn-active");
    }

    private void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = FxmlUtil.loader(fxmlPath);
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(title);
        } catch (Exception e) {
            System.err.println("Loi navigate: " + e.getMessage());
        }
    }
}

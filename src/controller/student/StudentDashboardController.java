package controller.student;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import util.FxmlUtil;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        String today = LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy",
            new java.util.Locale("vi", "VN")));
        lblPageDate.setText(today);
        loadDashboardData();
    }

    public void setUserInfo(String fullName, String studentId, String token) {
        lblUserName.setText(fullName.isEmpty() ? "Sinh vien" : fullName);
        lblUserId.setText("MSSV: " + studentId);
    }

    private void loadDashboardData() {
        lblTotalSubjects.setText("4");
        lblAttendanceRate.setText("87%");
        lblAbsentCount.setText("2");
        lblLateCount.setText("1");

        addScheduleItem("Lập trình mạng", "07:30 - 09:10", "P.201");
        addScheduleItem("Cơ sở dữ liệu",  "09:30 - 11:10", "P.305");

        addAttendanceItem("Lập trình mạng", "16/03/2026", "PRESENT");
        addAttendanceItem("Cơ sở dữ liệu",  "15/03/2026", "LATE");
        addAttendanceItem("Giải thuật",      "14/03/2026", "ABSENT");

        addNotificationItem("⚠️ Cảnh báo vắng học", "Môn Giải thuật: bạn đã vắng 3/10 buổi");
        addNotificationItem("📅 Thay đổi lịch học", "Môn CSDL dời sang thứ 4 tuần tới");
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

    @FXML private void showScheduleDay()     { setActiveBtn(btnScheduleDay);     lblPageTitle.setText("TKB theo ngày");       showComing(); }
    @FXML private void showScheduleWeek()    { setActiveBtn(btnScheduleWeek);    lblPageTitle.setText("TKB theo tuần");       showComing(); }
    @FXML private void showScheduleSubject() { setActiveBtn(btnScheduleSubject); lblPageTitle.setText("TKB theo môn");        showComing(); }
    @FXML private void showHistory()         { setActiveBtn(btnHistory);         lblPageTitle.setText("Lịch sử điểm danh");  showComing(); }
    @FXML private void showStats()           { setActiveBtn(btnStats);           lblPageTitle.setText("Thống kê chuyên cần"); showComing(); }
    @FXML private void showChat()            { setActiveBtn(btnChat);            lblPageTitle.setText("Chat lớp học");        showComing(); }
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

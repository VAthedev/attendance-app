package controller.lecturer;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class LecturerDashboardController implements Initializable {

    @FXML private Button btnDashboard, btnSchedule, btnOpenSession;
    @FXML private Button btnAttendanceList, btnStatistics, btnExport;
    @FXML private Button btnChat, btnNotification;

    @FXML private Label  lblPageTitle, lblPageDate;
    @FXML private Button btnQuickOpen;

    @FXML private Label lblUserName, lblUserCode;
    @FXML private Label lblTotalSubjects, lblTotalStudents;
    @FXML private Label lblAvgAttendance, lblAbsentWarning;

    @FXML private VBox  boxTodaySchedule, boxActiveSessions, boxAbsentWarning;
    @FXML private Label lblNoSchedule, lblNoSession, lblNoWarning;

    @FXML private ScrollPane paneDashboard;
    @FXML private Label      paneComingSoon;
    @FXML private StackPane  contentArea;

    private Node currentSubPane = null;
    private String sessionToken = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        String today = LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy",
            new java.util.Locale("vi", "VN")));
        lblPageDate.setText(today);
        loadDashboardData();
    }

    public void setUserInfo(String fullName, String userCode, String token) {
        this.sessionToken = token;
        lblUserName.setText(fullName.isEmpty() ? "Giang vien" : fullName);
        lblUserCode.setText("Ma GV: " + userCode);
    }

    private void loadDashboardData() {
        lblTotalSubjects.setText("3");
        lblTotalStudents.setText("87");
        lblAvgAttendance.setText("82%");
        lblAbsentWarning.setText("5");

        addScheduleItem("Lập trình mạng", "07:30 - 09:10", "P.201", "35 SV");
        addScheduleItem("Cơ sở dữ liệu",  "09:30 - 11:10", "P.305", "30 SV");

        addActiveSession("Lập trình mạng", "Đang mở", "08:45", 12, 35);

        addAbsentWarningRow("Nguyễn Văn A", "2151234567", "Lập trình mạng", 5, 10);
        addAbsentWarningRow("Trần Thị B",   "2151234568", "Cơ sở dữ liệu",  4, 10);
        addAbsentWarningRow("Lê Văn C",     "2151234569", "Giải thuật",      6, 12);
    }

    private void addScheduleItem(String subject, String time, String room, String students) {
        HBox item = new HBox(12);
        item.getStyleClass().add("schedule-item");
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label s = new Label(subject); s.getStyleClass().add("schedule-subject");
        Label t = new Label("⏰ " + time + "  🏫 " + room); t.getStyleClass().add("schedule-time");
        info.getChildren().addAll(s, t);
        Label sv = new Label("👥 " + students); sv.getStyleClass().add("schedule-room");
        item.getChildren().addAll(info, sv);
        boxTodaySchedule.getChildren().add(item);
        lblNoSchedule.setVisible(false); lblNoSchedule.setManaged(false);
    }

    private void addActiveSession(String subject, String status, String openTime,
                                   int checkedIn, int total) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color:#f0fdf4;-fx-background-radius:8;" +
                      "-fx-border-color:#86efac;-fx-border-radius:8;-fx-border-width:1;" +
                      "-fx-padding:12 14 12 14;");
        HBox top = new HBox(8);
        Label lblSubject = new Label(subject);
        lblSubject.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#14532d;");
        HBox.setHgrow(lblSubject, Priority.ALWAYS);
        Label lblStatus = new Label("● " + status);
        lblStatus.setStyle("-fx-text-fill:#16a34a;-fx-font-size:12px;-fx-font-weight:bold;");
        top.getChildren().addAll(lblSubject, lblStatus);
        Label lblInfo = new Label("Mở lúc " + openTime + "  •  " + checkedIn + "/" + total + " SV đã điểm danh");
        lblInfo.setStyle("-fx-font-size:12px;-fx-text-fill:#166534;");
        double pct = total > 0 ? (double) checkedIn / total : 0;
        StackPane progressBg = new StackPane();
        progressBg.setStyle("-fx-background-color:#dcfce7;-fx-background-radius:4;-fx-pref-height:6;");
        Region progressFill = new Region();
        progressFill.setStyle("-fx-background-color:#16a34a;-fx-background-radius:4;-fx-pref-height:6;");
        progressFill.setPrefWidth(240 * pct);
        StackPane.setAlignment(progressFill, javafx.geometry.Pos.CENTER_LEFT);
        progressBg.getChildren().add(progressFill);
        Button btnClose = new Button("Đóng phiên");
        btnClose.setStyle("-fx-background-color:#dc2626;-fx-text-fill:white;" +
                          "-fx-background-radius:6;-fx-font-size:12px;-fx-cursor:hand;");
        btnClose.setOnAction(e -> {
            boxActiveSessions.getChildren().remove(card);
            lblNoSession.setVisible(true);
        });
        card.getChildren().addAll(top, lblInfo, progressBg, btnClose);
        boxActiveSessions.getChildren().add(card);
        lblNoSession.setVisible(false); lblNoSession.setManaged(false);
    }

    private void addAbsentWarningRow(String name, String mssv, String subject,
                                      int absent, int total) {
        HBox row = new HBox(12);
        row.setStyle("-fx-padding:10 4 10 4;-fx-border-color:transparent transparent #e2e8f0 transparent;-fx-border-width:1;");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label n = new Label(name); n.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1a2744;");
        Label d = new Label(mssv + "  •  " + subject); d.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7a99;");
        info.getChildren().addAll(n, d);
        Label lblAbsent = new Label(absent + "/" + total + " buổi vắng");
        lblAbsent.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;" +
                           "-fx-background-radius:4;-fx-padding:3 8 3 8;-fx-font-size:12px;");
        Button btnEmail = new Button("📧 Gửi cảnh báo");
        btnEmail.setStyle("-fx-background-color:#fff7ed;-fx-text-fill:#c2410c;" +
                          "-fx-background-radius:6;-fx-font-size:11px;-fx-cursor:hand;");
        btnEmail.setOnAction(e -> { btnEmail.setText("✓ Đã gửi"); btnEmail.setDisable(true); });
        row.getChildren().addAll(info, lblAbsent, btnEmail);
        boxAbsentWarning.getChildren().add(row);
        lblNoWarning.setVisible(false); lblNoWarning.setManaged(false);
    }

    // ===== NAVIGATION =====

    @FXML private void showDashboard() {
        setActiveBtn(btnDashboard); lblPageTitle.setText("Tổng quan");
        removeSubPane();
        paneDashboard.setVisible(true);  paneDashboard.setManaged(true);
        paneComingSoon.setVisible(false); paneComingSoon.setManaged(false);
    }

    @FXML private void showOpenSession() {
        setActiveBtn(btnOpenSession); lblPageTitle.setText("Mở phiên điểm danh");
        loadSubPane("/fxml/lecturer/OpenSession.fxml");
    }

    @FXML private void showSchedule()       { setActiveBtn(btnSchedule);       lblPageTitle.setText("Quản lý TKB");          showComing(); }
    @FXML private void showAttendanceList() { setActiveBtn(btnAttendanceList); lblPageTitle.setText("Danh sách điểm danh");  showComing(); }
    @FXML private void showStatistics()     { setActiveBtn(btnStatistics);     lblPageTitle.setText("Thống kê chuyên cần");  showComing(); }
    @FXML private void showExport()         { setActiveBtn(btnExport);         lblPageTitle.setText("Xuất danh sách");       showComing(); }
    @FXML private void showChat()           { setActiveBtn(btnChat);           lblPageTitle.setText("Chat lớp học");         showComing(); }
    @FXML private void showNotification()   { setActiveBtn(btnNotification);   lblPageTitle.setText("Thông báo");            showComing(); }

    @FXML private void handleLogout() { loadScene("/fxml/auth/Login.fxml", "Dang nhap"); }

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            contentArea.getChildren().add(node);
            currentSubPane = node;
        } catch (Exception e) {
            System.err.println("Loi load pane: " + e.getMessage());
            e.printStackTrace();
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
        Button[] all = {btnDashboard, btnSchedule, btnOpenSession,
                        btnAttendanceList, btnStatistics, btnExport,
                        btnChat, btnNotification};
        for (Button b : all) b.getStyleClass().remove("sidebar-btn-active");
        active.getStyleClass().add("sidebar-btn-active");
    }

    private void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(title);
        } catch (Exception e) {
            System.err.println("Loi navigate: " + e.getMessage());
        }
    }
}

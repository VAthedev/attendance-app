package controller.lecturer;

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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import util.FxmlUtil;

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
    @FXML private StackPane  aiChatWidget;

    public static String currentLecturerId = "";
    public static String currentLecturerName = "";

    private Node currentSubPane = null;
    private String sessionToken = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        String today = LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy",
            new java.util.Locale("vi", "VN")));
        lblPageDate.setText(today);
        lblPageDate.setText(today);
        // loadDashboardData() will be called in setUserInfo
    }

    public void setUserInfo(String fullName, String userCode, String token) {
        this.sessionToken = token;
        lblUserName.setText(fullName.isEmpty() ? "Giang vien" : fullName);
        lblUserCode.setText("Ma GV: " + userCode.replace("}", ""));
        currentLecturerId = lblUserCode.getText().replace("Ma GV: ", "").trim();
        currentLecturerName = lblUserName.getText();

        // Đăng ký PushListener để nhận thông báo thời gian thực
        client.network.SocketClient.getInstance().addPushListener(res -> {
            if ("ANNOUNCEMENT".equals(res.getMessage()) && "SCHEDULE_UPDATED".equals(res.getDataValue("message"))) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Thông báo");
                alert.setHeaderText("Đồng bộ Thời khóa biểu");
                alert.setContentText("Thời khóa biểu đã có sự thay đổi. Vui lòng làm mới trang.");
                alert.show();
                loadDashboardData();
            }
        });
        
        javafx.application.Platform.runLater(this::loadDashboardData);
    }

    @FXML
    private void handleSyncTKB(javafx.event.ActionEvent event) {
        protocol.Request req = new protocol.Request(protocol.RequestType.SYNC_SCHEDULE);
        client.network.SocketClient.getInstance().sendAsync(req, res -> {
            if (res.isOk()) {
                System.out.println("Gửi yêu cầu đồng bộ thành công.");
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Lỗi");
                alert.setContentText(res.getMessage());
                alert.show();
            }
        });
    }

    private void loadDashboardData() {
        String lecturerName = lblUserName.getText();
        String lecturerId = lblUserCode.getText().replace("Ma GV: ", "").trim();
        LocalDate today = LocalDate.now();

        if (lecturerName == null || lecturerName.isEmpty() || "Giang vien".equals(lecturerName)) {
            return;
        }

        new Thread(() -> {
            try {
                // 1. Fetch Today's Schedules
                java.util.List<java.util.Map<String,Object>> schedules = database.ScheduleRepository.getInstance().findLecturerSchedulesByDate(lecturerName, today);
                
                com.mongodb.client.MongoCollection<org.bson.Document> enrollmentsCol = database.DatabaseHelper.getInstance().getEnrollmentsCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> attendanceCol = database.DatabaseHelper.getInstance().getAttendanceCollection();

                int totalStudentsAll = 0;
                
                final java.util.List<java.util.Map<String,String>> scheduleItemsData = new java.util.ArrayList<>();

                for (java.util.Map<String,Object> sch : schedules) {
                    String subject = (String) sch.get("subject");
                    String room = (String) sch.get("room");
                    String className = (String) sch.get("className");
                    
                    String periods = (String) sch.get("periods");
                    String time = "N/A";
                    if (periods != null) {
                        try {
                            String[] parts = periods.split(",");
                            int startP = Integer.parseInt(parts[0].trim());
                            int endP = Integer.parseInt(parts[parts.length-1].trim());
                            time = getPeriodTime(startP, true) + " - " + getPeriodTime(endP, false);
                        } catch (Exception e) {}
                    }

                    long studentCount = enrollmentsCol.countDocuments(com.mongodb.client.model.Filters.eq("subject_id", className));
                    totalStudentsAll += studentCount;

                    java.util.Map<String,String> item = new java.util.HashMap<>();
                    item.put("subject", subject);
                    item.put("time", time);
                    item.put("room", room);
                    item.put("students", studentCount + " SV");
                    scheduleItemsData.add(item);
                }
                final int finalTotalStudentsAll = totalStudentsAll;

                // 2. Fetch Active Sessions
                java.util.List<org.bson.Document> activeSessions = database.SessionRepository.getInstance().findActiveSessions(lecturerId);
                final java.util.List<java.util.Map<String,Object>> sessionItemsData = new java.util.ArrayList<>();
                
                for (org.bson.Document session : activeSessions) {
                    String subject = session.getString("subject");
                    long startMillis = session.getLong("start_time");
                    String openTime = java.time.LocalTime.ofInstant(java.time.Instant.ofEpochMilli(startMillis), java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"));
                    String sessionId = session.getString("_id");
                    if (sessionId == null && session.getObjectId("_id") != null) sessionId = session.getObjectId("_id").toHexString();

                    long checkedInCount = attendanceCol.countDocuments(com.mongodb.client.model.Filters.eq("session_id", sessionId));
                    
                    String className = session.getString("class_name");
                    long totalCount = enrollmentsCol.countDocuments(com.mongodb.client.model.Filters.eq("subject_id", className));

                    java.util.Map<String,Object> item = new java.util.HashMap<>();
                    item.put("subject", subject);
                    item.put("status", "Đang mở");
                    item.put("openTime", openTime);
                    item.put("checkedIn", (int)checkedInCount);
                    item.put("total", (int)totalCount);
                    sessionItemsData.add(item);
                }

                javafx.application.Platform.runLater(() -> {
                    lblTotalSubjects.setText(String.valueOf(schedules.size()));
                    lblTotalStudents.setText(String.valueOf(finalTotalStudentsAll));
                    lblAvgAttendance.setText("--%");
                    lblAbsentWarning.setText("0");

                    boxTodaySchedule.getChildren().clear();
                    if (scheduleItemsData.isEmpty()) {
                        lblNoSchedule.setVisible(true); lblNoSchedule.setManaged(true);
                    } else {
                        lblNoSchedule.setVisible(false); lblNoSchedule.setManaged(false);
                        for (java.util.Map<String,String> m : scheduleItemsData) {
                            addScheduleItem(m.get("subject"), m.get("time"), m.get("room"), m.get("students"));
                        }
                    }

                    boxActiveSessions.getChildren().clear();
                    if (sessionItemsData.isEmpty()) {
                        lblNoSession.setVisible(true); lblNoSession.setManaged(true);
                    } else {
                        lblNoSession.setVisible(false); lblNoSession.setManaged(false);
                        for (java.util.Map<String,Object> m : sessionItemsData) {
                            addActiveSession((String)m.get("subject"), (String)m.get("status"), (String)m.get("openTime"), (int)m.get("checkedIn"), (int)m.get("total"));
                        }
                    }

                    boxAbsentWarning.getChildren().clear();
                    lblNoWarning.setVisible(true); lblNoWarning.setManaged(true);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String getPeriodTime(int period, boolean isStart) {
        String[] startTimes = {"07:30", "08:20", "09:10", "10:00", "10:50", "13:00", "13:50", "14:40", "15:40", "16:30"};
        String[] endTimes = {"08:20", "09:10", "10:00", "10:50", "11:40", "13:50", "14:40", "15:30", "16:30", "17:20"};
        if (period < 1 || period > 10) return "00:00";
        return isStart ? startTimes[period - 1] : endTimes[period - 1];
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

    @FXML private void showSchedule()       { setActiveBtn(btnSchedule);       lblPageTitle.setText("Quản lý TKB");          loadSubPane("/fxml/lecturer/LecturerSchedule.fxml"); }
    @FXML private void showAttendanceList() { setActiveBtn(btnAttendanceList); lblPageTitle.setText("Danh sách điểm danh");  loadSubPane("/fxml/lecturer/LecturerAttendanceList.fxml"); }
    @FXML private void showStatistics()     { setActiveBtn(btnStatistics);     lblPageTitle.setText("Thống kê chuyên cần");  loadSubPane("/fxml/lecturer/LecturerStatistics.fxml"); }
    @FXML private void showExport()         { setActiveBtn(btnExport);         lblPageTitle.setText("Xuất danh sách");       loadSubPane("/fxml/lecturer/LecturerExport.fxml"); }
    @FXML private void showChat()           { setActiveBtn(btnChat);           lblPageTitle.setText("Chat nội bộ lớp học");  loadSubPane("/fxml/shared/Chat.fxml"); }
    @FXML private void showNotification()   { setActiveBtn(btnNotification);   lblPageTitle.setText("Thông báo");            loadSubPane("/fxml/student/Notification.fxml"); }

    @FXML private void handleLogout() { loadScene("/fxml/auth/Login.fxml", "Dang nhap"); }

    @FXML
    private void toggleAIChat() {
        boolean isVisible = aiChatWidget.isVisible();
        aiChatWidget.setVisible(!isVisible);
        aiChatWidget.setManaged(!isVisible);
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
            
            Object controller = loader.getController();
            if (controller instanceof LecturerScheduleController) {
                ((LecturerScheduleController) controller).setLecturerName(lblUserName.getText());
            } else if (controller instanceof LecturerAttendanceListController) {
                String uCode = lblUserCode.getText().replace("Ma GV: ", "").trim();
                ((LecturerAttendanceListController) controller).setLecturerId(uCode);
            } else if (controller instanceof OpenSessionController) {
                ((OpenSessionController) controller).setLecturerName(lblUserName.getText());
            } else if (controller instanceof LecturerStatisticsController) {
                ((LecturerStatisticsController) controller).setLecturerName(lblUserName.getText());
            } else if (controller instanceof LecturerExportController) {
                ((LecturerExportController) controller).setLecturerName(lblUserName.getText());
            }
            
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
            FXMLLoader loader = FxmlUtil.loader(fxmlPath);
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(title);
        } catch (Exception e) {
            System.err.println("Loi navigate: " + e.getMessage());
        }
    }
}
